package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import net.neoforged.neoforge.transfer.access.ItemAccess
import net.neoforged.neoforge.transfer.item.ItemResource
import net.neoforged.neoforge.transfer.item.ItemUtil
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper
import net.neoforged.neoforge.transfer.transaction.Transaction
import java.util.*
import kotlin.math.min

class ItemInteractivePayload(
    val itemStack: ItemStack,
    val count: Int,
    val take: Boolean,
    val targetSlot: Int,
    val context: HologramContext,
    val syncUUID: UUID
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<ItemInteractivePayload> = TYPE

    companion object {

        val TYPE = CustomPacketPayload.Type<ItemInteractivePayload>(HologramPanel.rl("item_interactive"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ItemInteractivePayload> = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ItemInteractivePayload::itemStack,
            ByteBufCodecs.VAR_INT, ItemInteractivePayload::count,
            ByteBufCodecs.BOOL, ItemInteractivePayload::take,
            ByteBufCodecs.INT, ItemInteractivePayload::targetSlot,
            HologramContext.STREAM_CODE, ItemInteractivePayload::context,
            UUIDUtil.STREAM_CODEC, ItemInteractivePayload::syncUUID,
            ::ItemInteractivePayload
        )

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val HANDLE = object : IPayloadHandler<ItemInteractivePayload> {
            override fun handle(payload: ItemInteractivePayload, payloadContext: IPayloadContext) {
                val cap = when (val context = payload.context) {
                    is BlockHologramContext ->
                        context.getLevel().getCapability(Capabilities.Item.BLOCK, context.pos, null)

                    is EntityHologramContext -> {
                        val entity = context.getEntity()
                        if (entity is ItemEntity) {
                            ItemAccess.forStack(entity.item).getCapability(Capabilities.Item.ITEM)
                        } else {
                            entity.getCapability(Capabilities.Item.ENTITY_AUTOMATION, null)
                        }
                    }
                } ?: return
                if (payload.targetSlot < 0) {
                    if (payload.take) {
                        var toTake = payload.count
                        for (index in 0..<cap.size()) {
                            val stored = ItemUtil.getStack(cap, index)
                            if (!ItemStack.isSameItemSameComponents(stored, payload.itemStack)) continue
                            Transaction.openRoot().use {
                                val extracted = cap.extract(index, ItemResource.of(stored),
                                    min(stored.count, toTake), it)
                                it.commit()
                                toTake -= extracted
                                giveItemToPlayer(ItemStack(stored.item, extracted), payloadContext.player() as ServerPlayer)
                                if (toTake <= 0) break
                            }
                        }
                    } else {
                        val player = payloadContext.player() as ServerPlayer
                        var storeItem = payload.itemStack
                        storeItem.count = payload.count
                        if (ItemStack.isSameItemSameComponents(player.mainHandItem, payload.itemStack)) {
                            for (index in 0..<cap.size()) {
                                if (storeItem.isEmpty) {
                                    break
                                } else {
                                    Transaction.openRoot().use {
                                        val inserted = cap.insert(index, ItemResource.of(storeItem), storeItem.count, it)
                                        storeItem = storeItem.copyWithCount(storeItem.count - inserted)
                                        it.commit()
                                    }
                                }
                            }
                            player.mainHandItem.count -= payload.count - storeItem.count
                            if (player.mainHandItem.isEmpty) {
                                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
                            } else {
                                player.setItemInHand(InteractionHand.MAIN_HAND, player.mainHandItem)
                            }
                        }
                        if (!storeItem.isEmpty) {
                            val playerInv = PlayerInventoryWrapper.of(player)
                            var storeIndex = 0
                            for (playerInvIndex in 0..playerInv.size()) {
                                val playerItem = ItemUtil.getStack(playerInv, playerInvIndex)
                                if (!ItemStack.isSameItemSameComponents(playerItem, storeItem)) continue
                                Transaction.openRoot().use {
                                    val ir = ItemResource.of(storeItem)
                                    var toStoreItem = playerInv.extract(playerInvIndex, ir, storeItem.count, it)
                                    it.commit()
                                    storeItem.count -= toStoreItem

                                    while (storeIndex < cap.size()) {
                                        toStoreItem -= cap.insert(storeIndex, ir, toStoreItem, it)
                                        it.commit()
                                        if (toStoreItem <= 0) {
                                            break
                                        } else {
                                            storeIndex++
                                        }
                                    }
                                    storeItem.count += toStoreItem
                                    playerInv.insert(playerInvIndex, ir, toStoreItem, it)
                                    it.commit()
                                    if (storeItem.count <= 0) return

                                    val current = cap.getAmountAsInt(storeIndex)
                                    if (current < cap.getCapacityAsInt(storeIndex, ir)) {
                                        storeIndex++
                                    } else break
                                }
                            }
                        }
                    }
                } else {
                    if (payload.targetSlot >= cap.size()) return
                    if (payload.take) {
                        val itemCanTake = ItemUtil.getStack(cap, payload.targetSlot)
                        if (!ItemStack.isSameItemSameComponents(itemCanTake, payload.itemStack)) return
                        val takeCount = min(itemCanTake.count, payload.count)
                        Transaction.openRoot().use {
                            val actualTake = cap.extract(payload.targetSlot, ItemResource.of(itemCanTake), takeCount, it)
                            it.commit()
                            giveItemToPlayer(ItemStack(itemCanTake.item, actualTake), payloadContext.player() as ServerPlayer)
                        }
                    } else {
                        val stored = ItemUtil.getStack(cap, payload.targetSlot)
                        if (!stored.isEmpty && !ItemStack.isSameItemSameComponents(stored, payload.itemStack)) return
                        var storeCount = if (stored.isEmpty) {
                            payload.count
                        } else {
                            min(stored.maxStackSize - stored.count, payload.count)
                        }

                        val player = payloadContext.player() as ServerPlayer
                        run {
                            val mainHandItem = player.mainHandItem
                            if (!ItemStack.isSameItemSameComponents(mainHandItem, stored)) return@run
                            val toStoreItem = mainHandItem.copyWithCount(min(storeCount, mainHandItem.count))
                            Transaction.openRoot().use {
                                val res = cap.insert(payload.targetSlot, ItemResource.of(toStoreItem), toStoreItem.count, it)
                                it.commit()
                                mainHandItem.count -= res
                                player.setItemInHand(InteractionHand.MAIN_HAND, mainHandItem)
                                storeCount -= res
                            }
                        }
                        if (storeCount > 0) {
                            val playerInv = VanillaContainerWrapper.of(player.inventory)
                            for (invIndex in 0..<playerInv.size()) {
                                val invItem = ItemUtil.getStack(playerInv, invIndex)
                                if (!ItemStack.isSameItemSameComponents(invItem, payload.itemStack)) continue
                                Transaction.openRoot().use {
                                    val ir = ItemResource.of(invItem)
                                    val extracted = playerInv.extract(invIndex, ir, storeCount, it)
                                    val res = cap.insert(payload.targetSlot, ir, extracted, it)
                                    it.commit()
                                    if (extracted - res > 0) {
                                        giveItemToPlayer(ItemStack(invItem.item, extracted - res), player)
                                        break
                                    }
                                    storeCount -= extracted
                                    if (storeCount == 0) break
                                }
                            }
                        }
                    }
                }
                DataQueryManager.Server.manualSync(payload.syncUUID, payloadContext.player() as ServerPlayer)
            }
        }

        private fun giveItemToPlayer(extracted: ItemStack, player: ServerPlayer) {
            if (extracted.isEmpty) return
            if (player.inventory.add(extracted)) return
            val itemEntity = player.drop(extracted, false)
            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay()
                itemEntity.target = player.uuid
            }
        }

        fun query(itemStack: ItemStack, count: Int, context: HologramContext, targetSlot: Int = -1) {
            if (count < -1) return
            if (targetSlot < 0 && count > itemStack.maxStackSize) return
            val syncUUID = DataQueryManager.Client.queryContextUUID(context) ?: return
            val payload = ItemInteractivePayload(itemStack, count, true, targetSlot, context, syncUUID)
            ClientPacketDistributor.sendToServer(payload)
        }

        fun store(itemStack: ItemStack, count: Int, context: HologramContext, targetSlot: Int = -1) {
            if (count < -1) return
            if (targetSlot < 0 && count > itemStack.maxStackSize) return
            val syncUUID = DataQueryManager.Client.queryContextUUID(context) ?: return
            val payload = ItemInteractivePayload(itemStack, count, false, targetSlot, context, syncUUID)
            ClientPacketDistributor.sendToServer(payload)
        }
    }
}
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
import net.neoforged.neoforge.items.wrapper.InvWrapper
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
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
                        context.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, context.pos, null)

                    is EntityHologramContext -> {
                        val entity = context.getEntity()
                        if (entity is ItemEntity) {
                            entity.item.getCapability(Capabilities.ItemHandler.ITEM)
                        } else {
                            entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, null)
                        }
                    }
                } ?: return
                if (payload.targetSlot < 0) {
                    if (payload.take) {
                        var toTake = payload.count
                        for (index in 0..<cap.slots) {
                            val stored = cap.getStackInSlot(index)
                            if (!ItemStack.isSameItemSameComponents(stored, payload.itemStack)) continue
                            val extracted = cap.extractItem(index, min(stored.count, toTake), false)
                            giveItemToPlayer(extracted, payloadContext.player() as ServerPlayer)
                            toTake -= extracted.count
                            if (toTake <= 0) break
                        }
                    } else {
                        val player = payloadContext.player() as ServerPlayer
                        var storeItem = payload.itemStack
                        storeItem.count = payload.count
                        if (ItemStack.isSameItemSameComponents(player.mainHandItem, payload.itemStack)) {
                            for (index in 0..<cap.slots) {
                                if (storeItem.isEmpty) {
                                    break
                                } else {
                                    storeItem = cap.insertItem(index, storeItem, false)
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
                            val playerInv = PlayerInvWrapper(player.inventory)
                            var storeIndex = 0
                            for (playerInvIndex in 0..playerInv.slots) {
                                val playerItem = playerInv.getStackInSlot(playerInvIndex)
                                if (!ItemStack.isSameItemSameComponents(playerItem, storeItem)) continue
                                var toStoreItem = playerInv.extractItem(playerInvIndex, storeItem.count, false)
                                storeItem.count -= toStoreItem.count
                                while (storeIndex < cap.slots) {
                                    toStoreItem = cap.insertItem(storeIndex, toStoreItem, false)
                                    if (toStoreItem.isEmpty) {
                                        break
                                    } else {
                                        storeIndex++
                                    }
                                }
                                storeItem.count += toStoreItem.count
                                playerInv.insertItem(playerInvIndex, toStoreItem, false)
                                if (storeItem.count <= 0) return

                                val current = cap.getStackInSlot(storeIndex)
                                if (current.count < cap.getSlotLimit(storeIndex)) {
                                    storeIndex++
                                }
                            }
                        }
                    }
                } else {
                    if (payload.targetSlot >= cap.slots) return
                    if (payload.take) {
                        val itemCanTake = cap.getStackInSlot(payload.targetSlot)
                        if (!ItemStack.isSameItemSameComponents(itemCanTake, payload.itemStack)) return
                        val takeCount = min(itemCanTake.count, payload.count)
                        val actualTake = cap.extractItem(payload.targetSlot, takeCount, false)
                        giveItemToPlayer(actualTake, payloadContext.player() as ServerPlayer)
                    } else {
                        val stored = cap.getStackInSlot(payload.targetSlot)
                        if (!stored.isEmpty && !ItemStack.isSameItemSameComponents(stored, payload.itemStack)) return
                        var storeCount = min(stored.maxStackSize - stored.count, payload.count)

                        val player = payloadContext.player() as ServerPlayer
                        run {
                            val mainHandItem = player.mainHandItem
                            if (!ItemStack.isSameItemSameComponents(mainHandItem, stored)) return@run
                            val toStoreItem = mainHandItem.copyWithCount(min(storeCount, mainHandItem.count))
                            val res = cap.insertItem(payload.targetSlot, toStoreItem, false)
                            mainHandItem.count = mainHandItem.count - (toStoreItem.count - res.count)
                            player.setItemInHand(InteractionHand.MAIN_HAND, mainHandItem)
                            storeCount -= (toStoreItem.count - res.count)
                        }
                        if (storeCount > 0) {
                            val playerInv = InvWrapper(player.inventory)
                            for (invIndex in 0..<playerInv.slots) {
                                val invItem = playerInv.getStackInSlot(invIndex)
                                if (!ItemStack.isSameItemSameComponents(invItem, payload.itemStack)) continue
                                val extracted = playerInv.extractItem(invIndex, storeCount, false)
                                val res = cap.insertItem(payload.targetSlot, extracted, false)
                                if (!res.isEmpty) {
                                    giveItemToPlayer(res, player)
                                    break
                                }
                                storeCount -= extracted.count
                                if (storeCount == 0) break
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
            if (count <= 0) return
            if (targetSlot < 0 && count > itemStack.maxStackSize) return
            val syncUUID = DataQueryManager.Client.queryContextUUID(context) ?: return
            val payload = ItemInteractivePayload(itemStack, count, true, targetSlot, context, syncUUID)
            PacketDistributor.sendToServer(payload)
        }

        fun store(itemStack: ItemStack, count: Int, context: HologramContext, targetSlot: Int = -1) {
            if (count <= 0) return
            if (targetSlot < 0 && count > itemStack.maxStackSize) return
            val syncUUID = DataQueryManager.Client.queryContextUUID(context) ?: return
            val payload = ItemInteractivePayload(itemStack, count, false, targetSlot, context, syncUUID)
            PacketDistributor.sendToServer(payload)
        }
    }
}
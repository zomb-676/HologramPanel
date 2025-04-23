package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.polyfill.*
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.items.wrapper.InvWrapper
import net.minecraftforge.items.wrapper.PlayerInvWrapper
import net.minecraftforge.network.NetworkEvent
import java.util.*
import kotlin.math.min

class ItemInteractivePayload(
    val itemStack: ItemStack,
    val count: Int,
    val take: Boolean,
    val targetSlot: Int,
    val context: HologramContext,
    val syncUUID: UUID
) : CustomPacketPayload<ItemInteractivePayload> {

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    companion object {

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ItemInteractivePayload> = StreamCodec.composite(
            ByteBufCodecs.ITEM_STACK, ItemInteractivePayload::itemStack,
            ByteBufCodecs.VAR_INT, ItemInteractivePayload::count,
            ByteBufCodecs.BOOL, ItemInteractivePayload::take,
            ByteBufCodecs.INT, ItemInteractivePayload::targetSlot,
            HologramContext.STREAM_CODE, ItemInteractivePayload::context,
            ByteBufCodecs.UUID, ItemInteractivePayload::syncUUID,
            ::ItemInteractivePayload
        )

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val HANDLE = object : IPayloadHandler<ItemInteractivePayload> {
            override fun handle(payload: ItemInteractivePayload, payloadContext: IPayloadContext) {
                val cap = when (val context = payload.context) {
                    is BlockHologramContext -> context.getBlockEntity()?.getCapability(ForgeCapabilities.ITEM_HANDLER) ?: return

                    is EntityHologramContext -> {
                        val entity = context.getEntity()
                        if (entity is ItemEntity) {
                            entity.item.getCapability(ForgeCapabilities.ITEM_HANDLER)
                        } else {
                            entity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                        }
                    }
                }.orElse(null) ?: return
                if (payload.targetSlot < 0) {
                    if (payload.take) {
                        var toTake = payload.count
                        for (index in 0..<cap.slots) {
                            val stored = cap.getStackInSlot(index)
                            if (!ItemStack.isSameItemSameTags(stored, payload.itemStack)) continue
                            val extracted = cap.extractItem(index, min(stored.count, toTake), false)
                            toTake -= extracted.count
                            giveItemToPlayer(extracted, payloadContext.player() as ServerPlayer)
                            if (toTake <= 0) break
                        }
                    } else {
                        val player = payloadContext.player() as ServerPlayer
                        var storeItem = payload.itemStack
                        storeItem.count = payload.count
                        if (ItemStack.isSameItemSameTags(player.mainHandItem, payload.itemStack)) {
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
                                if (!ItemStack.isSameItemSameTags(playerItem, storeItem)) continue
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
                                } else break
                            }
                        }
                    }
                } else {
                    if (payload.targetSlot >= cap.slots) return
                    if (payload.take) {
                        val itemCanTake = cap.getStackInSlot(payload.targetSlot)
                        if (!ItemStack.isSameItemSameTags(itemCanTake, payload.itemStack)) return
                        val takeCount = min(itemCanTake.count, payload.count)
                        val actualTake = cap.extractItem(payload.targetSlot, takeCount, false)
                        giveItemToPlayer(actualTake, payloadContext.player() as ServerPlayer)
                    } else {
                        val stored = cap.getStackInSlot(payload.targetSlot)
                        if (!stored.isEmpty && !ItemStack.isSameItemSameTags(stored, payload.itemStack)) return
                        var storeCount = if (stored.isEmpty) {
                            payload.count
                        } else {
                            min(stored.maxStackSize - stored.count, payload.count)
                        }

                        val player = payloadContext.player() as ServerPlayer
                        run {
                            val mainHandItem = player.mainHandItem
                            if (!ItemStack.isSameItemSameTags(mainHandItem, stored)) return@run
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
                                if (!ItemStack.isSameItemSameTags(invItem, payload.itemStack)) continue
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
                itemEntity.setTarget(player.uuid)
            }
        }

        fun query(itemStack: ItemStack, count: Int, context: HologramContext, targetSlot: Int = -1) {
            if (count < -1) return
            if (targetSlot < 0 && count > itemStack.maxStackSize) return
            val syncUUID = DataQueryManager.Client.queryContextUUID(context) ?: return
            val payload = ItemInteractivePayload(itemStack, count, true, targetSlot, context, syncUUID)
            payload.sendToServer()
        }

        fun store(itemStack: ItemStack, count: Int, context: HologramContext, targetSlot: Int = -1) {
            if (count < -1) return
            if (targetSlot < 0 && count > itemStack.maxStackSize) return
            val syncUUID = DataQueryManager.Client.queryContextUUID(context) ?: return
            val payload = ItemInteractivePayload(itemStack, count, false, targetSlot, context, syncUUID)
            payload.sendToServer()
        }
    }
}
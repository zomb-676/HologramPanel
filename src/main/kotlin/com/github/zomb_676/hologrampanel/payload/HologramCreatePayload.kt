package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.sync.DataSynchronizer
import com.github.zomb_676.hologrampanel.widget.interactive.DistType
import com.github.zomb_676.hologrampanel.widget.interactive.HologramInteractiveHelper
import com.github.zomb_676.hologrampanel.widget.interactive.HologramInteractiveTarget
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import java.util.*

class HologramCreatePayload<T : HologramInteractiveTarget>(
    val syncerUUID: UUID, val creator: HologramInteractiveTarget.Provider<T>, val additionData: ByteArray
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<HologramCreatePayload<*>>(HologramPanel.rl("syncer_create"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, HologramCreatePayload<*>> =
            object : StreamCodec<RegistryFriendlyByteBuf, HologramCreatePayload<*>> {
                override fun decode(buffer: RegistryFriendlyByteBuf): HologramCreatePayload<*> {
                    val uuid = UUIDUtil.STREAM_CODEC.decode(buffer)
                    val creator = AllRegisters.InteractiveHologramRegistry.STREAM_CODEC.decode(buffer)
                    val additionData = NeoForgeStreamCodecs.UNBOUNDED_BYTE_ARRAY.decode(buffer)
                    return HologramCreatePayload(uuid, creator, additionData)
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf, value: HologramCreatePayload<*>
                ) {
                    UUIDUtil.STREAM_CODEC.encode(buffer, value.syncerUUID)
                    AllRegisters.InteractiveHologramRegistry.STREAM_CODEC.encode(buffer, value.creator)
                    NeoForgeStreamCodecs.UNBOUNDED_BYTE_ARRAY.encode(buffer, value.additionData)
                }
            }

        val HANDLE = object : IPayloadHandler<HologramCreatePayload<*>> {
            override fun handle(payload: HologramCreatePayload<*>, context: IPayloadContext) {
                handlePacket(payload, context)
            }
        }

        private fun <T : HologramInteractiveTarget> handlePacket(
            payload: HologramCreatePayload<T>, context: IPayloadContext
        ) {
            require(context.player().uuid == Minecraft.getInstance().player!!.uuid)
            val player = context.player() as LocalPlayer
            val buffer = RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(payload.additionData),
                player.registryAccess(),
                context.listener().connectionType
            )
            val interactive: HologramInteractiveTarget =
                payload.creator.create(
                    player,
                    DistType.CLIENT,
                    DataSynchronizer(payload.syncerUUID, DistType.CLIENT, player.connection, player.registryAccess()),
                    buffer
                )
            val widget = HologramInteractiveHelper.create(interactive)
            val context = BlockHologramContext(TODO(), player, null)
            HologramInteractiveHelper.addClientWidget(widget, context)
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
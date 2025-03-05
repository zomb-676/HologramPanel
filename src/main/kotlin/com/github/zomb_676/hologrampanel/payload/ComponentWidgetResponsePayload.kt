package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidgetRequesterManager
import io.netty.buffer.Unpooled
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import java.util.*

class ComponentWidgetResponsePayload(val widgetUUID: UUID, val buffer: ByteArray) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<ComponentWidgetResponsePayload>(HologramPanel.rl("component_widget_response"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentWidgetResponsePayload> = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ComponentWidgetResponsePayload::widgetUUID,
            NeoForgeStreamCodecs.UNBOUNDED_BYTE_ARRAY, ComponentWidgetResponsePayload::buffer,
            ::ComponentWidgetResponsePayload
        )
        val HANDLE = object : IPayloadHandler<ComponentWidgetResponsePayload> {
            override fun handle(
                payload: ComponentWidgetResponsePayload,
                context: IPayloadContext
            ) {
                val player = context.player() as LocalPlayer
                val buffer = RegistryFriendlyByteBuf(
                    Unpooled.wrappedBuffer(payload.buffer),
                    player.registryAccess(), player.connection.connectionType
                )
                HologramComponentWidgetRequesterManager.Client.handle(payload.widgetUUID, buffer)
            }
        }

        fun response(player: ServerPlayer, buf: RegistryFriendlyByteBuf, uuid: UUID) {
            val payload = ComponentWidgetResponsePayload(uuid, buf.asByteBuf().array())
            player.connection.send(payload)
        }
    }

    override fun type(): CustomPacketPayload.Type<out ComponentWidgetResponsePayload> = TYPE
}
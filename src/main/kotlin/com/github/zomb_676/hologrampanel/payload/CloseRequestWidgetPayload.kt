package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidgetRequesterManager
import net.minecraft.client.Minecraft
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import java.util.*

class CloseRequestWidgetPayload(val uuid: UUID) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<CloseRequestWidgetPayload>(HologramPanel.rl("close_request_widget"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, CloseRequestWidgetPayload> = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CloseRequestWidgetPayload::uuid,
            ::CloseRequestWidgetPayload
        )
        val HANDLE = object : IPayloadHandler<CloseRequestWidgetPayload> {
            override fun handle(
                payload: CloseRequestWidgetPayload,
                context: IPayloadContext
            ) {
                HologramComponentWidgetRequesterManager.Server.onWidgetClose(
                    payload.uuid,
                    context.player() as ServerPlayer
                )
            }
        }

        /**
         * call on logic client side
         */
        fun close(uuid: UUID) {
            Minecraft.getInstance().player!!.connection.send(CloseRequestWidgetPayload(uuid))
        }
    }

    override fun type(): CustomPacketPayload.Type<out CloseRequestWidgetPayload> = TYPE
}
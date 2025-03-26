package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.DebugHelper
import com.github.zomb_676.hologrampanel.HologramPanel
import net.minecraft.client.Minecraft
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler

class QueryDebugStatisticsPayload(val enable: Boolean) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<QueryDebugStatisticsPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<QueryDebugStatisticsPayload>(HologramPanel.rl("query_debug_statistics"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, QueryDebugStatisticsPayload> = StreamCodec.composite(
            ByteBufCodecs.BOOL, QueryDebugStatisticsPayload::enable,
            ::QueryDebugStatisticsPayload
        )
        val HANDLE = object : IPayloadHandler<QueryDebugStatisticsPayload> {
            override fun handle(
                payload: QueryDebugStatisticsPayload,
                context: IPayloadContext
            ) {
                DebugHelper.Server.onPacket(payload, context)
            }
        }

        fun query(state: Boolean) {
            if (!state) {
                DebugStatisticsPayload.clear()
            }
            val player = Minecraft.getInstance().player ?: return
            player.connection.send(QueryDebugStatisticsPayload(state))
        }
    }
}
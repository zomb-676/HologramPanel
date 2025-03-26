package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler

class DebugStatisticsPayload(val total: Int, val forPlayer: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<DebugStatisticsPayload> = TYPE

    companion object {
        var TOTAL_SYNC_COUNT: Int = -1
            private set
        var SYNC_COUNT_FOR_PLAYER: Int = -1
            private set

        val TYPE = CustomPacketPayload.Type<DebugStatisticsPayload>(HologramPanel.rl("debug_statistics"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DebugStatisticsPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DebugStatisticsPayload::total,
            ByteBufCodecs.VAR_INT, DebugStatisticsPayload::forPlayer,
            ::DebugStatisticsPayload
        )
        val HANDLE = object : IPayloadHandler<DebugStatisticsPayload> {
            override fun handle(
                payload: DebugStatisticsPayload,
                context: IPayloadContext
            ) {
                TOTAL_SYNC_COUNT = payload.total
                SYNC_COUNT_FOR_PLAYER = payload.forPlayer
            }
        }
        fun clear() {
            TOTAL_SYNC_COUNT = -1
            SYNC_COUNT_FOR_PLAYER = -1
        }
    }
}
package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.polyfill.*
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent

/**
 * debug statistics data sent server to client
 *
 * @param total total count for HologramWidget to sync
 * @param forPlayer sync count request by the corresponding player
 */
class DebugStatisticsPayload(val total: Int, val forPlayer: Int) : CustomPacketPayload<DebugStatisticsPayload> {

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    companion object {
        var TOTAL_SYNC_COUNT: Int = -1
            private set
        var SYNC_COUNT_FOR_PLAYER: Int = -1
            private set

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DebugStatisticsPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DebugStatisticsPayload::total,
            ByteBufCodecs.VAR_INT, DebugStatisticsPayload::forPlayer,
            ::DebugStatisticsPayload
        )
        val HANDLE = object : IPayloadHandler<DebugStatisticsPayload> {
            override fun handle(payload: DebugStatisticsPayload, context: IPayloadContext) {
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
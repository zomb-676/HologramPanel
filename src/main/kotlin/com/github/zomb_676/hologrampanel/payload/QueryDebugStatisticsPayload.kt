package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.DebugHelper
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.IPayloadContext
import com.github.zomb_676.hologrampanel.polyfill.IPayloadHandler
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import net.minecraft.client.Minecraft
import net.minecraftforge.network.NetworkEvent

/**
 * notify the servet to begin/end send statistics data to clinet
 */
class QueryDebugStatisticsPayload(val enable: Boolean) : CustomPacketPayload<QueryDebugStatisticsPayload> {

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, QueryDebugStatisticsPayload> = StreamCodec.composite(
            ByteBufCodecs.BOOL, QueryDebugStatisticsPayload::enable,
            ::QueryDebugStatisticsPayload
        )
        val HANDLE = object : IPayloadHandler<QueryDebugStatisticsPayload> {
            override fun handle(payload: QueryDebugStatisticsPayload, context: IPayloadContext) {
                DebugHelper.Server.onPacket(payload, context)
            }
        }

        fun query(state: Boolean) {
            if (!state) {
                DebugStatisticsPayload.clear()
            }
            QueryDebugStatisticsPayload(state).sendToServer()
        }
    }
}
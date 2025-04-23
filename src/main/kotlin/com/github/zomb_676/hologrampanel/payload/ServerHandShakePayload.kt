package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.IPayloadContext
import com.github.zomb_676.hologrampanel.polyfill.IPayloadHandler
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent

/**
 * handshake packet to let the client know this mod is installed at server side
 */
class ServerHandShakePayload(val id: Int = 0) : CustomPacketPayload<ServerHandShakePayload> {

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    companion object {
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ServerHandShakePayload> = StreamCodec.composite(
            ByteBufCodecs.INT, ServerHandShakePayload::id,
            ::ServerHandShakePayload
        )
        val HANDLE = object : IPayloadHandler<ServerHandShakePayload> {
            override fun handle(payload: ServerHandShakePayload, context: IPayloadContext) {
                HologramPanel.serverInstalled = true
            }
        }
    }
}
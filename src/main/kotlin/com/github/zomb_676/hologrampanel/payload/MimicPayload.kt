package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.polyfill.IPayloadContext
import com.github.zomb_676.hologrampanel.polyfill.IPayloadHandler
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import net.minecraftforge.network.NetworkEvent

/**
 * the payload, which is used when origin payload decoding failed, just makes the decoding system happy
 */
object MimicPayload : CustomPacketPayload<MimicPayload> {

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MimicPayload> = object : StreamCodec<RegistryFriendlyByteBuf, MimicPayload> {
        override fun decode(buffer: RegistryFriendlyByteBuf): MimicPayload {
            return MimicPayload
        }

        override fun encode(buffer: RegistryFriendlyByteBuf, value: MimicPayload) {}
    }
    val HANDLE = object : IPayloadHandler<MimicPayload> {
        override fun handle(payload: MimicPayload, context: IPayloadContext) {
        }
    }
}
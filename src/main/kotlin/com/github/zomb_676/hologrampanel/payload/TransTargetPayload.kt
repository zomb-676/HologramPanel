package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.trans.TransEntry
import com.github.zomb_676.hologrampanel.trans.TransSource
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler

class TransTargetPayload<S1 : Any, S2 : Any>(
    val transQuery: TransSource<S1>,
    val transStore: TransSource<S2>,
    val transEntry: TransEntry<S1, S2, *, *>
) :
    CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<TransTargetPayload<*, *>> = TYPE

    companion object {
        private fun create(
            transQuery: TransSource<*>,
            transStore: TransSource<*>,
            transEntry: TransEntry<*, *, *, *>
        ): TransTargetPayload<Any, Any> = TransTargetPayload(
            transQuery.unsafeCast(),
            transStore.unsafeCast(),
            transEntry.unsafeCast(),
        )

        val TYPE = CustomPacketPayload.Type<TransTargetPayload<*, *>>(HologramPanel.rl("trans_target_payload"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TransTargetPayload<*, *>> = StreamCodec.composite(
            TransSource.STREAM_CODEC, TransTargetPayload<*, *>::transStore,
            TransSource.STREAM_CODEC, TransTargetPayload<*, *>::transQuery,
            TransEntry.STREAM_CODEC, TransTargetPayload<*, *>::transEntry,
            ::create
        )
        val HANDLE = object : IPayloadHandler<TransTargetPayload<*, *>> {
            override fun handle(payload: TransTargetPayload<*, *>, context: IPayloadContext) {
                handlePayload(payload)
            }
        }

        private fun <S1 : Any, S2 : Any> handlePayload(payload: TransTargetPayload<S1, S2>) {
            val query = payload.transQuery.getTarget() ?: return
            val store = payload.transStore.getTarget() ?: return
            payload.transEntry.run(query, store)
        }
    }
}
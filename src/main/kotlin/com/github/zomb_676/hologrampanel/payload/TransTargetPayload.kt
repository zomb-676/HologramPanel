package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.polyfill.IPayloadContext
import com.github.zomb_676.hologrampanel.polyfill.IPayloadHandler
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import com.github.zomb_676.hologrampanel.trans.TransOperation
import com.github.zomb_676.hologrampanel.trans.TransSource
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraftforge.network.NetworkEvent

class TransTargetPayload<S1 : Any, S2 : Any>(
    val transQuery: TransSource<S1>,
    val transStore: TransSource<S2>,
    val transOperation: TransOperation<S1, S2, *, *, *>
) : CustomPacketPayload<TransTargetPayload<S1,S2>>{

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    companion object {
        private fun create(
            transQuery: TransSource<*>,
            transStore: TransSource<*>,
            transOperation: TransOperation<*, *, *, *, *>
        ): TransTargetPayload<Any, Any> = TransTargetPayload(
            transQuery,
            transStore,
            transOperation.unsafeCast(),
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TransTargetPayload<*, *>> = StreamCodec.composite(
            TransSource.STREAM_CODEC, TransTargetPayload<*, *>::transQuery,
            TransSource.STREAM_CODEC, TransTargetPayload<*, *>::transStore,
            TransOperation.STREAM_CODEC, TransTargetPayload<*, *>::transOperation,
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
            payload.transOperation.run(query, store)
        }
    }

    override fun sendToServer() {
        val queryCount = transOperation.queryPath.count
        if (queryCount <= 0) return
        val storeCount = transOperation.storePath.count
        if (storeCount <= 0) return
        if (storeCount < queryCount) {
            transOperation.queryPath.count = storeCount
        }
        super.sendToServer()
    }
}
package com.github.zomb_676.hologrampanel.trans

import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

data class TransEntry<S1 : Any, S2 : Any, H : Any, R : Any>(
    val query: TransTarget<S1, H, R>,
    val queryPath: TransPath<H, R>,
    val store: TransTarget<S2, H, R>,
    val storePath: TransPath<H, R>,
) {
    companion object {
        private fun create(
            query: TransTarget<*, *, *>,
            queryPath: TransPath<*, *>,
            store: TransTarget<*, *, *>,
            storePath: TransPath<*, *>
        ): TransEntry<Any, Any, Any, Any> {
            return TransEntry(
                query.unsafeCast(),
                queryPath.unsafeCast(),
                store.unsafeCast(),
                storePath.unsafeCast()
            )
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TransEntry<*, *, *, *>> = StreamCodec.composite(
            TransTarget.TransTargetStreamCodec, TransEntry<*, *, *, *>::query,
            TransPath.STREAM_CODEC, TransEntry<*, *, *, *>::queryPath,
            TransTarget.TransTargetStreamCodec, TransEntry<*, *, *, *>::store,
            TransPath.STREAM_CODEC, TransEntry<*, *, *, *>::storePath,
            ::create
        )
    }

    fun run(querySource: S1, storeSource: S2) {
        val queryActual = this.query.queryActual(querySource, this.queryPath) ?: return
        val remain = this.store.storeActual(storeSource, this.storePath, queryActual) ?: return
        //TODO process remain
    }
}
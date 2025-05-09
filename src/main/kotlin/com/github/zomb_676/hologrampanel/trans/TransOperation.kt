package com.github.zomb_676.hologrampanel.trans

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.payload.TransTargetPayload
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

/**
 * define the whole transformation chain, including query and store
 */
data class TransOperation<in S1 : Any, in S2 : Any, H1 : Any, H2 : Any, R : Any>(
    val query: TransHandle<S1, H1>,
    val queryPath: TransPath<H1, R>,
    val store: TransHandle<S2, H2>,
    val storePath: TransPath<H2, R>,
) {
    companion object {

        fun <S1 : Any, S2 : Any, H1 : Any, H2 : Any, R : Any> create(
            query: Triple<TransSource<S1>, TransHandle<S1, H1>, TransPath<H1, R>>,
            store: Triple<TransSource<S2>, TransHandle<S2, H2>, TransPath<H2, R>>
        ): TransTargetPayload<S1, S2> {
            val operation = TransOperation(query.second, query.third, store.second, store.third)
            return TransTargetPayload(query.first, store.first, operation)
        }

        private fun create(
            query: TransHandle<*, *>,
            queryPath: TransPath<*, *>,
            store: TransHandle<*, *>,
            storePath: TransPath<*, *>
        ): TransOperation<Any, Any, Any, Any, Any> {
            return TransOperation(
                query.unsafeCast(),
                queryPath.unsafeCast(),
                store.unsafeCast(),
                storePath.unsafeCast()
            )
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TransOperation<*, *, *, *, *>> = StreamCodec.composite(
            TransHandle.TransTargetStreamCodec, TransOperation<*, *, *, *, *>::query,
            TransPath.STREAM_CODEC, TransOperation<*, *, *, *, *>::queryPath,
            TransHandle.TransTargetStreamCodec, TransOperation<*, *, *, *, *>::store,
            TransPath.STREAM_CODEC, TransOperation<*, *, *, *, *>::storePath,
            ::create
        )
    }

    fun run(querySource: S1, storeSource: S2) {
        //test first to avoid something is extracted but can't be inserted
        val queryTest = this.query.queryTest(querySource, this.queryPath) ?: return
        if (this.queryPath.isEmpty(queryTest)) return
        val remainTest = this.store.storeTest(storeSource, this.storePath, queryTest) ?: return
        if (!this.queryPath.isEmpty(remainTest) && this.queryPath.getCount(remainTest) > 0) {
            //correct the query count
            val transCount = this.queryPath.getCount(queryTest) - this.storePath.getCount(remainTest)
            if (transCount <= 0) return
            this.queryPath.count = transCount
            this.storePath.count = transCount
        }

        //do actual operation
        val queryActual = this.query.queryActual(querySource, this.queryPath) ?: return
        val remain = this.store.storeActual(storeSource, this.storePath, queryActual) ?: return
        if (this.storePath.getCount(remain) > 0) {
            HologramPanel.LOGGER.info("found something that is not inserted successfully")
        }
    }
}
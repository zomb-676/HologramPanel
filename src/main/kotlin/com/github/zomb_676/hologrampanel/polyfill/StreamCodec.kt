package com.github.zomb_676.hologrampanel.polyfill

import com.mojang.datafixers.util.Function3
import com.mojang.datafixers.util.Function4
import com.mojang.datafixers.util.Function6
import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiFunction
import java.util.function.Function

typealias RegistryFriendlyByteBuf = FriendlyByteBuf

interface StreamCodec<in B, T> {
    fun decode(buffer: B): T
    fun encode(buffer: B, value: T)

    companion object {
        fun <B, C, T1> composite(
            codec1: StreamCodec<in B, T1>,
            getter1: Function<C, T1>,
            factory: Function<T1, C>
        ): StreamCodec<B, C> {
            return object : StreamCodec<B, C> {
                override fun decode(buffer: B): C {
                    val t1 = codec1.decode(buffer)
                    return factory.apply(t1)
                }

                override fun encode(buffer: B, value: C) {
                    codec1.encode(buffer, getter1.apply(value))
                }
            }
        }

        fun <B, C, T1, T2> composite(
            codec1: StreamCodec<in B, T1>,
            getter1: Function<C, T1>,
            codec2: StreamCodec<in B, T2>,
            getter2: Function<C, T2>,
            factory: BiFunction<T1, T2, C>
        ): StreamCodec<B, C> {
            return object : StreamCodec<B, C> {
                override fun decode(buffer: B): C {
                    val t1 = codec1.decode(buffer)
                    val t2 = codec2.decode(buffer)
                    return factory.apply(t1, t2)
                }

                override fun encode(buffer: B, value: C) {
                    codec1.encode(buffer, getter1.apply(value))
                    codec2.encode(buffer, getter2.apply(value))
                }
            }
        }

        fun <B, C, T1, T2, T3> composite(
            codec1: StreamCodec<in B, T1>,
            getter1: Function<C, T1>,
            codec2: StreamCodec<in B, T2>,
            getter2: Function<C, T2>,
            codec3: StreamCodec<in B, T3>,
            getter3: Function<C, T3>,
            factory: Function3<T1, T2, T3, C>
        ): StreamCodec<B, C> {
            return object : StreamCodec<B, C> {
                override fun decode(buffer: B): C {
                    val t1 = codec1.decode(buffer)
                    val t2 = codec2.decode(buffer)
                    val t3 = codec3.decode(buffer)
                    return factory.apply(t1, t2, t3)
                }

                override fun encode(buffer: B, value: C) {
                    codec1.encode(buffer, getter1.apply(value))
                    codec2.encode(buffer, getter2.apply(value))
                    codec3.encode(buffer, getter3.apply(value))
                }
            }
        }

        fun <B, C, T1, T2, T3, T4> composite(
            codec1: StreamCodec<in B, T1>,
            getter1: Function<C, T1>,
            codec2: StreamCodec<in B, T2>,
            getter2: Function<C, T2>,
            codec3: StreamCodec<in B, T3>,
            getter3: Function<C, T3>,
            codec4: StreamCodec<in B, T4>,
            getter4: Function<C, T4>,
            factory: Function4<T1, T2, T3, T4, C>
        ): StreamCodec<B, C> {
            return object : StreamCodec<B, C> {
                override fun decode(buffer: B): C {
                    val t1 = codec1.decode(buffer)
                    val t2 = codec2.decode(buffer)
                    val t3 = codec3.decode(buffer)
                    val t4 = codec4.decode(buffer)
                    return factory.apply(t1, t2, t3, t4)
                }

                override fun encode(buffer: B, value: C) {
                    codec1.encode(buffer, getter1.apply(value))
                    codec2.encode(buffer, getter2.apply(value))
                    codec3.encode(buffer, getter3.apply(value))
                    codec4.encode(buffer, getter4.apply(value))
                }
            }
        }

        fun <B, C, T1, T2, T3, T4, T5, T6> composite(
            codec1: StreamCodec<in B, T1>,
            getter1: Function<C, T1>,
            codec2: StreamCodec<in B, T2>,
            getter2: Function<C, T2>,
            codec3: StreamCodec<in B, T3>,
            getter3: Function<C, T3>,
            codec4: StreamCodec<in B, T4>,
            getter4: Function<C, T4>,
            codec5: StreamCodec<in B, T5>,
            getter5: Function<C, T5>,
            codec6: StreamCodec<in B, T6>,
            getter6: Function<C, T6>,
            factory: Function6<T1, T2, T3, T4, T5, T6, C>
        ): StreamCodec<B, C> {
            return object : StreamCodec<B, C> {
                public override fun decode(buffer: B): C {
                    val t1 = codec1.decode(buffer)
                    val t2 = codec2.decode(buffer)
                    val t3 = codec3.decode(buffer)
                    val t4 = codec4.decode(buffer)
                    val t5 = codec5.decode(buffer)
                    val t6 = codec6.decode(buffer)
                    return factory.apply(t1, t2, t3, t4, t5, t6)
                }

                public override fun encode(buffer: B, value: C) {
                    codec1.encode(buffer, getter1.apply(value))
                    codec2.encode(buffer, getter2.apply(value))
                    codec3.encode(buffer, getter3.apply(value))
                    codec4.encode(buffer, getter4.apply(value))
                    codec5.encode(buffer, getter5.apply(value))
                    codec6.encode(buffer, getter6.apply(value))
                }
            }
        }
    }
}
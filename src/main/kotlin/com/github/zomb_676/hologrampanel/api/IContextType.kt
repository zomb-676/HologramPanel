package com.github.zomb_676.hologrampanel.api

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

interface IContextType<T : Any> : StreamCodec<RegistryFriendlyByteBuf, T> {
    fun append(data: T, buffer: RegistryFriendlyByteBuf)
    fun query(buffer: RegistryFriendlyByteBuf): T

    override fun decode(buffer: RegistryFriendlyByteBuf): T {
        return this.query(buffer)
    }

    override fun encode(buffer: RegistryFriendlyByteBuf, value: T) {
        this.append(value, buffer)
    }

    companion object {
        fun <T : Any> fromStreamCodec(streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>): IContextType<T> {
            return object : IContextType<T> {
                override fun append(data: T, buffer: RegistryFriendlyByteBuf) {
                    streamCodec.encode(buffer, data)
                }

                override fun query(buffer: RegistryFriendlyByteBuf): T {
                    return streamCodec.decode(buffer)
                }

                override fun decode(buffer: RegistryFriendlyByteBuf): T {
                    return streamCodec.decode(buffer)
                }

                override fun encode(buffer: RegistryFriendlyByteBuf, value: T) {
                    streamCodec.encode(buffer, value)
                }
            }
        }

        val BLOCK_POS = fromStreamCodec(BlockPos.STREAM_CODEC)
    }
}
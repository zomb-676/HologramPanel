package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.api.IContextType
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import javax.swing.Icon

class ContextHolder(val map: MutableMap<IContextType<*>, Any> = mutableMapOf()) {
    companion object {
        val STREAM_CODE: StreamCodec<RegistryFriendlyByteBuf, ContextHolder> =
            object : StreamCodec<RegistryFriendlyByteBuf, ContextHolder> {
                override fun decode(buffer: RegistryFriendlyByteBuf): ContextHolder {
                    val count = buffer.readVarInt()
                    val map = mutableMapOf<IContextType<*>, Any>()
                    repeat(count) { _ ->
                        val contextType = AllRegisters.ContextTypeRegistry.STREAM_CODEC.decode(buffer)
                        val data = contextType.decode(buffer)
                        map[contextType] = data
                    }
                    return ContextHolder(map)
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf,
                    value: ContextHolder
                ) {
                    val map = value.map
                    buffer.writeVarInt(map.size)
                    fun <T : Any> encode(type : IContextType<T>, data : Any) {
                        type.encode(buffer, data.unsafeCast())
                    }
                    map.forEach { (type, data) ->
                        AllRegisters.ContextTypeRegistry.STREAM_CODEC.encode(buffer, type)
                        encode(type, data)
                    }
                }
            }
    }

    fun <T : Any> append(type: IContextType<T>, value: T) {
        if (!map.containsKey(type)) {
            map[type] = value
        }
    }

    fun <T : Any> query(type: IContextType<T>): T {
        return map[type]!!.unsafeCast()
    }
}
package com.github.zomb_676.hologrampanel.payload

import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

abstract class MessageEntry<T>(val type: Class<T>) {

    companion object {
        private var GLOBAL_INDEX = 0
    }

    val index: Int = GLOBAL_INDEX++

    abstract fun encode(instance: T, buf: FriendlyByteBuf)

    abstract fun decode(buf: FriendlyByteBuf): T

    abstract fun handle(instance: T, context: Supplier<NetworkEvent.Context>)

    open fun direction(): NetworkDirection? {
        return null
    }
}
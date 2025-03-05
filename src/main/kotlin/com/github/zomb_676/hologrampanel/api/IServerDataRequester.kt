package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.widget.component.ContextHolder
import net.minecraft.network.RegistryFriendlyByteBuf

interface IServerDataRequester {
    fun getProcessor(): IServerDataProcessor

    /**
     * this will run on logic client side
     */
    fun onServerDataReceived(buf: RegistryFriendlyByteBuf)

    fun appendContext(context: ContextHolder)
}
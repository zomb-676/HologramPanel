package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.widget.component.ContextHolder
import net.minecraft.network.RegistryFriendlyByteBuf

interface IServerDataProcessor {
    /**
     * this will run on logic server side
     */
    fun appendServerData(context: ContextHolder, buf: RegistryFriendlyByteBuf)
}
package com.github.zomb_676.hologrampanel.api

import net.minecraft.network.RegistryFriendlyByteBuf

interface IServerDataRequired {
    fun appendServerData(buf: RegistryFriendlyByteBuf)
    fun getServerData(buf: RegistryFriendlyByteBuf)
    fun isWaiting(): Boolean
}
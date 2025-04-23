package com.github.zomb_676.hologrampanel.polyfill

import net.minecraft.network.protocol.PacketFlow
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent

@JvmInline
value class IPayloadContext(private val context: NetworkEvent.Context) {
    fun flow() = when (context.direction) {
        NetworkDirection.PLAY_TO_SERVER -> PacketFlow.SERVERBOUND
        NetworkDirection.PLAY_TO_CLIENT -> PacketFlow.CLIENTBOUND
        else -> throw RuntimeException("Unhandled direction $context")
    }

    fun player() = context.sender
}
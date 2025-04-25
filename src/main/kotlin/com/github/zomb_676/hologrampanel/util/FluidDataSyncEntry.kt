package com.github.zomb_676.hologrampanel.util

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.neoforged.neoforge.fluids.FluidStack

data class FluidDataSyncEntry(val fluidStack: FluidStack, val max: Int) {

    val current get() = fluidStack.amount

    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidDataSyncEntry> =
            object : StreamCodec<RegistryFriendlyByteBuf, FluidDataSyncEntry> {
                override fun decode(buffer: RegistryFriendlyByteBuf): FluidDataSyncEntry {
                    val fluidStack = FluidStack.STREAM_CODEC.decode(buffer)
                    val max = ByteBufCodecs.VAR_INT.decode(buffer)
                    return FluidDataSyncEntry(fluidStack, max)
                }

                override fun encode(buffer: RegistryFriendlyByteBuf, value: FluidDataSyncEntry) {
                    FluidStack.STREAM_CODEC.encode(buffer, value.fluidStack)
                    ByteBufCodecs.VAR_INT.encode(buffer, value.max)
                }
            }
    }
}
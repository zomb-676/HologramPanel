package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import net.minecraftforge.fluids.FluidStack


data class FluidDataSyncEntry(val fluidStack: FluidStack, val max: Int) {

    val current get() = fluidStack.amount

    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidDataSyncEntry> =
            object : StreamCodec<RegistryFriendlyByteBuf, FluidDataSyncEntry> {
                override fun decode(buffer: RegistryFriendlyByteBuf): FluidDataSyncEntry {
                    val fluidStack = ByteBufCodecs.FLUID_STACK.decode(buffer)
                    val max = ByteBufCodecs.VAR_INT.decode(buffer)
                    return FluidDataSyncEntry(fluidStack, max)
                }

                override fun encode(buffer: RegistryFriendlyByteBuf, value: FluidDataSyncEntry) {
                    ByteBufCodecs.FLUID_STACK.encode(buffer, value.fluidStack)
                    ByteBufCodecs.VAR_INT.encode(buffer, value.max)
                }
            }
    }
}
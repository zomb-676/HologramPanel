package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import net.minecraftforge.fluids.FluidType

data class FluidDataSyncEntry(val type: FluidType, val current: Int, val max: Int) {
    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidDataSyncEntry> =
            object : StreamCodec<RegistryFriendlyByteBuf, FluidDataSyncEntry> {
                override fun decode(buffer: RegistryFriendlyByteBuf): FluidDataSyncEntry {
                    val type = AllRegisters.Codecs.FLUID_TYPE_STREAM_CODEC.decode(buffer)
                    val current = ByteBufCodecs.VAR_INT.decode(buffer)
                    val max = ByteBufCodecs.VAR_INT.decode(buffer)
                    return FluidDataSyncEntry(type, current, max)
                }

                override fun encode(buffer: RegistryFriendlyByteBuf, value: FluidDataSyncEntry) {
                    AllRegisters.Codecs.FLUID_TYPE_STREAM_CODEC.encode(buffer, value.type)
                    ByteBufCodecs.VAR_INT.encode(buffer, value.current)
                    ByteBufCodecs.VAR_INT.encode(buffer, value.max)
                }
            }
    }
}
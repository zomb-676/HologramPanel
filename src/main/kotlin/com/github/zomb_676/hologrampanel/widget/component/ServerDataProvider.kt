package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import net.minecraft.nbt.CompoundTag

interface ServerDataProvider<T : HologramContext> {
    fun updateServerData(additionData: CompoundTag, targetData: CompoundTag, context: T)
    fun additionInformationForServer(additionData: CompoundTag)
}
package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import net.minecraft.nbt.CompoundTag

interface ServerDataProvider<T : HologramContext> : ComponentProvider<T> {
    /**
     * run on logic server
     */
    fun appendServerData(additionData: CompoundTag, targetData: CompoundTag, context: T)

    /**
     * run on logic client, this only called once
     */
    fun additionInformationForServer(additionData: CompoundTag, context : T) {}
}
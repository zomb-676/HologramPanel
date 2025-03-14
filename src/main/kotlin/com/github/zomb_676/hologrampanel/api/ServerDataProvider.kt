package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import net.minecraft.nbt.CompoundTag

/**
 * implementation this if your [ComponentProvider] require server data
 */
interface ServerDataProvider<T : HologramContext> : ComponentProvider<T> {
    /**
     * run on logic server
     *
     * @return will sync the append data to client when the return value is true
     *
     * use condition return if you want to control network usage or just always true for simplicity
     */
    fun appendServerData(additionData: CompoundTag, targetData: CompoundTag, context: T) : Boolean

    /**
     * run on logic client, this only called once, [appendServerData] will receive this
     *
     * you can add more information here to customize the data you want
     */
    fun additionInformationForServer(additionData: CompoundTag, context : T) {}
}
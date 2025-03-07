package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import net.minecraft.resources.ResourceLocation

interface ComponentProvider<T : HologramContext> {

    /**
     * this only called once when is is created
     */
    fun appendComponent(builder: HologramWidgetBuilder<T>)

    @EfficientConst
    fun targetClass(): Class<*>

    @EfficientConst
    fun location(): ResourceLocation
}
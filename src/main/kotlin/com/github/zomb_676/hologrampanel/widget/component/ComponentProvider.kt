package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.resources.ResourceLocation

interface ComponentProvider<T : HologramContext> {

    /**
     * this is called when any data changed or ar displayType change
     */
    fun appendComponent(builder: HologramWidgetBuilder<T>, displayType: DisplayType)

    @EfficientConst
    fun targetClass(): Class<*>

    @EfficientConst
    fun location(): ResourceLocation
}
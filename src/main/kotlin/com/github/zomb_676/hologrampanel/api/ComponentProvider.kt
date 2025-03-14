package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.resources.ResourceLocation

/**
 * attach component to a specific game object
 *
 * use [ServerDataProvider] if you need data from server
 *
 * @param T the context whe widget is at
 */
interface ComponentProvider<T : HologramContext> {

    /**
     * this is called when any data changed or ar displayType change
     */
    fun appendComponent(builder: HologramWidgetBuilder<T>, displayType: DisplayType)

    /**
     * @return the game object class you want to display
     */
    @EfficientConst
    fun targetClass(): Class<*>

    /**
     * @return identity object for debug and customize
     */
    @EfficientConst
    fun location(): ResourceLocation
}
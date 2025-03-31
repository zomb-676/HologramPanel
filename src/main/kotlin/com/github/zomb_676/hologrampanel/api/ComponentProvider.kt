package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.resources.ResourceLocation

/**
 * attach component to a specific game object
 *
 * implement [ServerDataProvider] if you need data from server
 *
 * this class is singleton, so if you want to store field for each widget, use
 * [com.github.zomb_676.hologrampanel.widget.dynamic.Remember.keep]
 *
 * @param T the context whe widget is at
 */
interface ComponentProvider<T : HologramContext, V> {

    /**
     * this is called when any data changed or ar displayType change
     */
    fun appendComponent(builder: HologramWidgetBuilder<T>, displayType: DisplayType)

    /**
     * @return the game object class you want to display.
     * must be the type represented by the corresponding context
     */
    @EfficientConst
    fun targetClass(): Class<V>

    /**
     * @return identity object for debug and customize
     */
    @EfficientConst
    fun location(): ResourceLocation

    /**
     * this will prevent the returned [ComponentProvider] if both you are in a same candidate list
     *
     * the replaced target will still apply replacement
     *
     * it is intended to replace a universal implementation to your customized implementation
     *
     * only called during collecting providers based on class
     */
    @EfficientConst
    fun replaceProvider(target: ResourceLocation): Boolean = false

    /**
     * for performance implementation, if you know only some targets or some targets never need this
     *
     * only called during collecting providers based on class
     */
    @EfficientConst
    fun appliesTo(context: T, check: V): Boolean = true

    fun attachTicket(context: T, tick: TicketAdder<T>) {}
}
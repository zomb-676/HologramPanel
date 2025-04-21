package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size

/**
 * the basic hologram widget interface, just indicates a sized widget render in level or on screen
 */
interface HologramWidget {

    /**
     * measure widget size
     */
    fun measure(style: HologramStyle, displayType: DisplayType): Size

    /**
     * do render
     */
    fun render(state: HologramRenderState, style: HologramStyle, displayType: DisplayType, partialTicks: Float)

    fun closeWidget() {
        HologramManager.remove(this)
        this.onRemove()
    }

    fun onRemove() {}
    fun onAdd(state: HologramRenderState) {}

    /**
     * if the hologram only has ordinaryContent, we can skip render it to save performance
     */
    fun hasNoneOrdinaryContent(): Boolean = true
}
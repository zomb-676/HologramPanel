package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size

/**
 * the basic hologram widget interface
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

    fun onSelected() {}
    fun onDisSelected() {}

    fun closeWidget() {
        HologramManager.remove(this)
        this.onRemove()
    }

    fun onRemove() {}
    fun onAdd() {}

    fun hasNoneOrdinaryContent() : Boolean = true
}
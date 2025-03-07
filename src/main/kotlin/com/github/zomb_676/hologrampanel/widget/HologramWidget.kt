package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramState
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size
import org.jetbrains.annotations.ApiStatus

interface HologramWidget {

    /**
     * measure widget size
     */
    fun measure(style: HologramStyle, displayType: DisplayType): Size

    /**
     * do render
     */
    fun render(state: HologramState, style: HologramStyle, displayType: DisplayType, partialTicks: Float)

    fun onSelected() {}
    fun onDisSelected() {}

    @ApiStatus.NonExtendable
    fun closeWidget() {
        HologramManager.remove(this)
        this.onRemove()
    }

    fun onRemove() {}
    fun onAdd() {}
}
package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramState
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size

abstract class HologramWidget {
    enum class DisplayType {
        FOCUSED, MAXIMUM, NORMAL, MINIMAL, SCREEN_EDGE
    }

    var locateType: LocateType = LocateType.Screen.Free
    var displayType: DisplayType = DisplayType.NORMAL

    abstract fun render(state: HologramState, style: HologramStyle, partialTicks: Float)
    abstract fun measure(displayType: DisplayType, style: HologramStyle): Size

    open fun onSelected() {}
    open fun onDisSelected() {}

    fun closeWidget() {
        HologramManager.remove(this)
        this.onRemove()
    }

    open fun onRemove() {}
    open fun onAdd() {}
}
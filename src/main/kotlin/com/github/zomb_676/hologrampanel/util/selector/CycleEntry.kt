package com.github.zomb_676.hologrampanel.util.selector

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size

/**
 * entry interface to define operation and render in [CycleSelector]
 *
 * not implementation this in mose case
 */
sealed interface CycleEntry {
    fun onClick(callback: SelectorCallback, trigType: TrigType)
    fun onClose(callback: SelectorCallback) {}
    fun renderContent(style: HologramStyle, partialTick: Float, isHover: Boolean)
    fun size(style: HologramStyle): Size
    fun scale(): Double

    fun tick() {}
    fun isVisible(): Boolean = true

    interface Group : CycleEntry {
        fun children(): List<CycleEntry>
        fun childrenCount() = children().size
    }

    interface Single : CycleEntry

    interface SelectorCallback {
        fun openGroup(group: Group)
        fun recoveryToParent()
    }

    data object EmptyCallback : SelectorCallback {
        override fun openGroup(group: Group) {}

        override fun recoveryToParent() {}
    }

    enum class TrigType {
        BY_CLICK,
        BY_RELEASE
    }
}
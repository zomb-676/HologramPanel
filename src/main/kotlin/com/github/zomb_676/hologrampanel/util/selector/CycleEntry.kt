package com.github.zomb_676.hologrampanel.util.selector

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size

sealed interface CycleEntry {
    fun onClick(callback : SelectorCallback)
    fun onClose()
    fun renderContent(style: HologramStyle, partialTick: Float, isHover: Boolean)
    fun size(style: HologramStyle): Size


    interface Group : CycleEntry {
        fun children(): List<CycleEntry>
        fun childrenCount() = children().size
    }

    interface Single : CycleEntry

    interface SelectorCallback {
        fun openGroup(group: Group)
        fun recoveryToParent(child : CycleEntry)
    }

    data object EmptyCallback : SelectorCallback {
        override fun openGroup(group: Group) {}

        override fun recoveryToParent(child: CycleEntry) {}
    }
}
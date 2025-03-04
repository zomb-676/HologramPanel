package com.github.zomb_676.hologrampanel.widget.interactive

import com.github.zomb_676.hologrampanel.widget.HologramWidget

abstract class HologramInteractiveWidget<T : HologramInteractiveTarget>(val target: T) : HologramWidget() {
    abstract val traceSource: Any
}
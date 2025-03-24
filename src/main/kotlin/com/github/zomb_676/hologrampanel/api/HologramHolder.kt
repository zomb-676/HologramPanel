package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.widget.HologramWidget

/**
 * can be used for none-singleton class
 */
interface HologramHolder {
    fun setWidget(widget : HologramWidget?) : HologramWidget?
    fun getWidget() : HologramWidget?
}
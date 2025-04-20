package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.widget.HologramWidget

/**
 * can be used for none-singleton class
 */
interface HologramHolder {
    fun `hologramPanel$setWidget`(widget: HologramWidget?): HologramWidget?
    fun `hologramPanel$getWidget`(): HologramWidget?
}
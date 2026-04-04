package com.github.zomb_676.hologrampanel.api.event

import com.github.zomb_676.hologrampanel.render.HologramStyle
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * use this event to set the style used for render hologram
 */
class StyleCreateEvent(private val guiGraphics: GuiGraphicsExtractor) : IHologramEvent() {
    fun getGuiGraphics(): GuiGraphicsExtractor = guiGraphics

    private var style : HologramStyle = HologramStyle.DefaultStyle(guiGraphics)

    fun setStyle(style: HologramStyle) {
        this.style = style
    }

    fun getStyle(): HologramStyle = this.style
}
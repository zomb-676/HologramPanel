package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw

object InteractionLayer {
    fun getLayer(): LayeredDraw.Layer = object : LayeredDraw.Layer {
        override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) =
            HologramManager.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))
    }
}
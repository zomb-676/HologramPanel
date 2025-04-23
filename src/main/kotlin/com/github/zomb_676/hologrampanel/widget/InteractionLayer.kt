package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.util.glDebugStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay

object InteractionLayer {
    fun getLayer(): IGuiOverlay = object : IGuiOverlay {
        override fun render(gui: ForgeGui, guiGraphics: GuiGraphics, partialTick: Float, screenWidth: Int, screenHeight: Int) {
            glDebugStack("renderHologramOverlayPart") {
                HologramManager.renderOverlayPart(guiGraphics, partialTick)
            }
        }
    }
}
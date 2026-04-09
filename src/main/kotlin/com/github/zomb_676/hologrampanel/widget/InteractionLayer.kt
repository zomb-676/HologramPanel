package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.EventHandler
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager.dragData
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.util.glDebugStack
import com.github.zomb_676.hologrampanel.util.stack
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.gui.GuiLayer

object InteractionLayer {
    fun getLayer(): GuiLayer = GuiLayer { guiGraphics, deltaTracker ->
        val wrs = Minecraft.getInstance().gameRenderer.gameRenderState.windowRenderState
        if (wrs.isResized) {
            EventHandler.ClientOnly.onWindowResize(wrs.width, wrs.height)
        }
        glDebugStack("renderHologramOverlayPart") {
            HologramManager.renderOverlayPart(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))
        }
    }

    fun getDraggingLayer(): GuiLayer = object : GuiLayer {
        override fun render(guiGraphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
            val dragging = dragData ?: return
            if (dragging.isStillValid() && dragging.isDataOfType<ItemStack>()) {
                val item = dragging.getTypedDragData<ItemStack>() ?: return
                guiGraphics.stack {
                    val window = Minecraft.getInstance().window
                    guiGraphics.pose().translate(window.guiScaledWidth / 2.0f + Config.Style.dragPromptXOffset.get().toFloat(),
                        window.guiScaledHeight / 2.0f + Config.Style.dragPromptYOffset.get().toFloat())
//                    RenderSystem.setShaderColor(1f, 1f, 1f, Config.Style.dragPromptAlpha.get().toFloat())// TODO
                    guiGraphics.item(item, 0, 0)
                    guiGraphics.itemDecorations(Minecraft.getInstance().font, item, 0, 0)
//                    guiGraphics.flush()
//                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                }
            }
        }
    }
}
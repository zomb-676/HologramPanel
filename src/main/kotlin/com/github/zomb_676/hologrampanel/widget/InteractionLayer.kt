package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager.dragData
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.util.glDebugStack
import com.github.zomb_676.hologrampanel.util.stack
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.world.item.ItemStack

object InteractionLayer {
    fun getLayer(): LayeredDraw.Layer = object : LayeredDraw.Layer {
        override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
            glDebugStack("renderHologramOverlayPart") {
                HologramManager.renderOverlayPart(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))
            }
        }
    }

    fun getDraggingLayer(): LayeredDraw.Layer = object : LayeredDraw.Layer {
        override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
            val dragging = dragData ?: return
            if (dragging.isStillValid() && dragging.isDataOfType<ItemStack>()) {
                val item = dragging.getTypedDragData<ItemStack>() ?: return
                guiGraphics.stack {
                    val window = Minecraft.getInstance().window
                    guiGraphics.pose().translate(window.guiScaledWidth / 2.0 + Config.Style.dragPromptXOffset.get(),
                        window.guiScaledHeight / 2.0 + Config.Style.dragPromptYOffset.get(),
                        0.0)
                    RenderSystem.setShaderColor(1f, 1f, 1f, Config.Style.dragPromptAlpha.get().toFloat())
                    guiGraphics.renderItem(item, 0, 0)
                    guiGraphics.renderItemDecorations(Minecraft.getInstance().font, item, 0, 0)
                    guiGraphics.flush()
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                }
            }
        }
    }
}
package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.util.ARGB
import net.neoforged.neoforge.client.event.ClientTickEvent

object InteractionLayer {
    fun getLayer(): LayeredDraw.Layer = object : LayeredDraw.Layer {
        override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) =
            HologramManager.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))
    }

    var command: InteractionCommand.Exact? = null
        private set
    var remainTimeInTicks: Int = 0
        private set

    fun updateExactCommand(command: InteractionCommand.Exact?) {
        if (command != null) {
            this.command = command
            this.remainTimeInTicks = 40
        }
    }

    fun tick(event: ClientTickEvent) {
        if (remainTimeInTicks > 0) {
            --remainTimeInTicks
        }
    }

    fun renderCommand(x: Int, y: Int, guiGraphics: GuiGraphics, partialTick: Float) {
        if (remainTimeInTicks > 0 && command != null) {
            val color = ARGB.lerp((this.remainTimeInTicks + partialTick) / 40f, 0x00ffffff.toInt(), -1)
            guiGraphics.drawString(Minecraft.getInstance().font, command.toString(), x, y, color)
        }
    }
}
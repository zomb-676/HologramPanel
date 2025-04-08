package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.client.event.InputEvent
import org.joml.Matrix4f
import org.joml.Vector4f

/**
 * wrapper class, record required parameters for interact operations
 *
 * @param interactiveSize coordinate based on measured
 * @param poseMatrix the matrix used to draw the interactive content, can be changed safely
 *
 * @property mouseX same coordinate as [interactiveSize]
 * @property mouseY same coordinate as [interactiveSize]
 */
class InteractiveEntry(
    val interactive: HologramInteractive, val context: HologramContext, val interactiveSize: Size, poseMatrix: Matrix4f
) {
    val mouseX: Int
    val mouseY: Int
    val inverseMatrix: Matrix4f = poseMatrix.invert()

    init {
        val window = Minecraft.getInstance().window
        val mouseX = window.guiScaledWidth / 2
        val mouseY = window.guiScaledHeight / 2
        val vector = Vector4f(mouseX.toFloat(), mouseY.toFloat(), 0f, 1f)
        inverseMatrix.transform(vector)
        this.mouseX = vector.x.toInt()
        this.mouseY = vector.y.toInt()
    }

    fun onKey(event: InputEvent.Key): Boolean {
        if (Config.Server.allowHologramInteractive.get()) {
            val player = Minecraft.getInstance().player ?: return false
            val key = HologramInteractive.Key.create(event)
            return interactive.onKey(player, key, context, interactiveSize, mouseX, mouseY)
        } else {
            Minecraft.getInstance().gui.setOverlayMessage(FORBIDEN_COMPONENT, false)
            return true
        }
    }

    fun onMouseScroll(event: InputEvent.MouseScrollingEvent): Boolean {
        if (Config.Server.allowHologramInteractive.get()) {
            val player = Minecraft.getInstance().player ?: return false
            val mouseScroll = HologramInteractive.MouseScroll.create(event)
            return interactive.onMouseScroll(player, mouseScroll, context, interactiveSize, mouseX, mouseY)
        } else {
            Minecraft.getInstance().gui.setOverlayMessage(FORBIDEN_COMPONENT, false)
            return true
        }
    }

    fun onMouseClick(event: InputEvent.MouseButton): Boolean {
        if (Config.Server.allowHologramInteractive.get()) {
            val player = Minecraft.getInstance().player ?: return false
            val button = HologramInteractive.MouseButton.create(event)
            return interactive.onMouseClick(player, button, context, interactiveSize, mouseX, mouseY)
        } else {
            Minecraft.getInstance().gui.setOverlayMessage(FORBIDEN_COMPONENT, false)
            return true
        }
    }

    fun renderInteractive(style: HologramStyle, widgetSize: Size, partialTicks: Float) {
        val hint = Config.Client.displayInteractiveHint.get()
        interactive.renderInteractive(style, context, widgetSize, interactiveSize, mouseX, mouseY, partialTicks, hint)
    }

    override fun toString(): String {
        return "InteractiveEntry(interactiveSize=$interactiveSize, mouseX=$mouseX, mouseY=$mouseY)"
    }

    companion object {

        val FORBIDEN_COMPONENT = Component.literal("Interactive Is Disabled On This Server")

        /**
         * @param size the size the interactive take
         * @param hologramStyle query actual position from its matrix
         */
        fun of(
            interactive: HologramInteractive, context: HologramContext, size: Size, hologramStyle: HologramStyle
        ): InteractiveEntry = InteractiveEntry(interactive, context, size, Matrix4f(hologramStyle.poseMatrix()))
    }
}
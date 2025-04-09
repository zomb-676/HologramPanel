package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
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

    fun onKey(data: HologramInteractionManager.Key, player: LocalPlayer): Boolean {
        return interactive.onKey(player, data, context, interactiveSize, mouseX, mouseY)
    }

    fun onMouseScroll(data: HologramInteractionManager.MouseScroll, player: LocalPlayer): Boolean {
        return interactive.onMouseScroll(player, data, context, interactiveSize, mouseX, mouseY)
    }

    fun onMouseClick(data: HologramInteractionManager.MouseButton, player: LocalPlayer): Boolean {
        return interactive.onMouseClick(player, data, context, interactiveSize, mouseX, mouseY)
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
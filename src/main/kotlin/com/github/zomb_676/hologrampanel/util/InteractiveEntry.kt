package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import org.joml.Matrix4f
import org.joml.Vector4f

/**
 * wrapper class, record required parameters for interact operations
 *
 * @property interactive should be instance of [RebuildValue]
 * @property interactiveSize coordinate based on measured, not influenced by [poseMatrix]
 * @property poseMatrix the matrix used to draw the interactive content, can be changed safely
 *
 * @property mouseX same coordinate as [interactiveSize]
 * @property mouseY same coordinate as [interactiveSize]
 */
class InteractiveEntry internal constructor(
    val container: Any,
    val interactive: HologramInteractive,
    val context: HologramContext,
    val interactiveSize: Size,
    val poseMatrix: Matrix4f,
    val parent: InteractiveEntry? = null
) {

    fun getLatestInteractiveEntry(): HologramInteractive? {
        return this.interactive.unsafeCast<RebuildValue<IRenderElement?>>().getCurrent()?.unsafeCast<HologramInteractive>()
    }

    val mouseX: Int
    val mouseY: Int
    val inverseMatrix: Matrix4f = poseMatrix.invert()

    init {
        require(interactive is RebuildValue<*>)
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

    fun trigDrag(player: LocalPlayer): HologramInteractionManager.DragDataContext<*>? {
        return interactive.onTrigDrag(player, context, interactiveSize, mouseX, mouseY)
    }

    fun onDragPass(dragDataContext: HologramInteractionManager.DragDataContext<*>) {
        interactive.onDragPass(dragDataContext, context, interactiveSize, mouseX, mouseY)
    }

    fun onDragTransform(dragDataContext: HologramInteractionManager.DragDataContext<*>) {
        interactive.onDragTransform(dragDataContext,context, interactiveSize, mouseX, mouseY)
    }

    fun renderInteractive(style: HologramStyle, widgetSize: Size, partialTicks: Float) {
        val hint = Config.Client.displayInteractiveHint.get()
        interactive.renderInteractive(style, context, widgetSize, interactiveSize, mouseX, mouseY, partialTicks, hint)
    }

    override fun toString(): String {
        return "InteractiveEntry(interactiveSize=$interactiveSize, mouseX=$mouseX, mouseY=$mouseY)"
    }
}
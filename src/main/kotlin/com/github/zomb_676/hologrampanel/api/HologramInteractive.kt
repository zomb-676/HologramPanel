package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager.Key
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager.MouseButton
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager.MouseScroll
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Player

/**
 * define all the supported interaction for hologram
 */
interface HologramInteractive {
    /**
     * only be called when [org.lwjgl.glfw.GLFW.GLFW_RELEASE] happened
     *
     * @param data mouse related operation data
     * @param interactiveSize the size the interactive target that takes
     * @param mouseX mouse position related to left-up of interactive target
     * @param mouseY mouse position related to left-up of interactive target
     *
     * @return true will consume the input, will prevent [net.minecraftforge.client.event.InputEvent.MouseButton]
     */
    fun onMouseClick(player: LocalPlayer, data: MouseButton, context: HologramContext, interactiveSize: Size, mouseX: Int, mouseY: Int): Boolean =
        false

    /**
     * @param data mouse related operation data
     * @param interactiveSize the size the interactive target that takes
     * @param mouseX mouse position related to left-up of interactive target
     * @param mouseY mouse position related to left-up of interactive target
     *
     * @return true will consume the input, will prevent [net.minecraftforge.client.event.InputEvent.MouseScrollingEvent]
     */
    fun onMouseScroll(player: LocalPlayer, data: MouseScroll, context: HologramContext, interactiveSize: Size, mouseX: Int, mouseY: Int): Boolean =
        false

    /**
     * @param data mouse related operation data
     * @param interactiveSize the size the interactive target that takes
     * @param mouseX mouse position related to left-up of interactive target
     * @param mouseY mouse position related to left-up of interactive target
     *
     * @return true will consume the input
     */
    fun onKey(player: LocalPlayer, data: Key, context: HologramContext, interactiveSize: Size, mouseX: Int, mouseY: Int): Boolean = false

    /**
     * this is called when a drag begin operation happens
     *
     * @return null means not trig drag
     */
    fun onTrigDrag(
        player: Player,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ): HologramInteractionManager.DragDataContext<*>? = null

    /**
     * this is called when the drag data move over the interactive target
     */
    fun onDragPass(
        dragDataContext: HologramInteractionManager.DragDataContext<*>,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ) {

    }

    /**
     * this is called when mouse released over the interactive target, call [HologramInteractionManager.DragDataContext.consumeDrag]
     */
    fun onDragTransform(
        dragDataContext: HologramInteractionManager.DragDataContext<*>,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ) {

    }

    /**
     * this is called when mouse is over this, used to render some tips
     *
     * @param renderInteractiveHint a flag that set by user to decide to render a hint or not
     * this function is called not take this flag into consideration, you can skip render something
     * that is not important when the flag is true
     */
    fun renderInteractive(
        style: HologramStyle,
        context: HologramContext,
        widgetSize: Size,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
        renderInteractiveHint: Boolean
    ) {
    }
}
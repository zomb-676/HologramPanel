package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size
import net.minecraft.client.player.LocalPlayer
import net.neoforged.neoforge.client.event.InputEvent
import org.jetbrains.annotations.ApiStatus

interface HologramInteractive {
    /**
     * @return true will consume the input
     */
    fun onMouseClick(
        player: LocalPlayer,
        data: MouseButton,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ): Boolean = false

    /**
     * @return true will consume the input
     */
    fun onMouseScroll(
        player: LocalPlayer,
        data: MouseScroll,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ): Boolean = false

    /**
     * @return true will consume the input
     */
    fun onKey(
        player: LocalPlayer, data: Key, context: HologramContext, interactiveSize: Size, mouseX: Int, mouseY: Int
    ): Boolean = false

    fun renderInteractive(
        style: HologramStyle,
        context: HologramContext,
        widgetSize: Size,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ) {}

    data class Key(
        val key: Int, val scanCode: Int, val action: Int, val modifiers: Int
    ) {
        companion object {
            @ApiStatus.Internal
            internal fun create(event: InputEvent.Key): Key {
                return Key(event.key, event.scanCode, event.action, event.modifiers)
            }
        }
    }

    data class MouseScroll(
        val scrollDeltaX: Double,
        val scrollDeltaY: Double,
        val mouseX: Double,
        val mouseY: Double,
        val leftDown: Boolean,
        val middleDown: Boolean,
        val rightDown: Boolean,
    ) {
        companion object {
            @ApiStatus.Internal
            internal fun create(event: InputEvent.MouseScrollingEvent): MouseScroll {
                return MouseScroll(
                    event.scrollDeltaX,
                    event.scrollDeltaY,
                    event.mouseX,
                    event.mouseY,
                    event.isLeftDown,
                    event.isMiddleDown,
                    event.isRightDown
                )
            }
        }
    }

    data class MouseButton(
        val button: Int,
        val action: Int,
        val modifiers: Int,
    ) {
        companion object {
            @ApiStatus.Internal
            internal fun create(event: InputEvent.MouseButton): MouseButton {
                return MouseButton(event.button, event.action, event.modifiers)
            }
        }
    }
}
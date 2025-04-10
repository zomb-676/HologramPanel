package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.ItemInteractivePayload
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

open class InteractiveItemElement(item: ItemStack, val interactiveSlot: Int) : ItemStackElement(true, item),
    HologramInteractive {

    override fun render(style: HologramStyle, partialTicks: Float) {
        if (itemStack.isEmpty) {
            style.outline(style.itemStackSize())
        } else super.render(style, partialTicks)
    }

    override fun onMouseClick(
        player: LocalPlayer,
        data: HologramInteractionManager.MouseButton,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ): Boolean {
        val isShiftDown = data.modifiers and GLFW.GLFW_MOD_SHIFT != 0
        when (data.button) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                val count = if (isShiftDown) itemStack.count else 1
                if (!this.itemStack.isEmpty) {
                    ItemInteractivePayload.Companion.query(itemStack, count, context, interactiveSlot)
                }
            }

            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                if (this.itemStack.isEmpty) {
                    val mainHand = player.mainHandItem
                    if (!mainHand.isEmpty) {
                        val count = if (isShiftDown) mainHand.count else 1
                        ItemInteractivePayload.Companion.store(mainHand, count, context, interactiveSlot)
                    }
                } else {
                    val count = if (isShiftDown) this.itemStack.maxStackSize - this.itemStack.count else 1
                    ItemInteractivePayload.Companion.store(this.itemStack, count, context, interactiveSlot)
                }
            }
        }
        return true
    }
}
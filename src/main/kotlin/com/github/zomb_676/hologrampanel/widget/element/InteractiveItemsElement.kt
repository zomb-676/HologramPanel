package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.ItemInteractivePayload
import com.github.zomb_676.hologrampanel.util.Size
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import kotlin.math.min

open class InteractiveItemsElement(items: List<ItemStack>, itemEachLine: Int = 7, addition: Boolean = true) :
    ItemsElement(items, itemEachLine, addition) {
    override fun onMouseClick(
        player: LocalPlayer,
        data: HologramInteractionManager.MouseButton,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ): Boolean {
        val index = decodeIndex(mouseX, mouseY)
        val shiftDown = data.modifiers and GLFW.GLFW_MOD_SHIFT != 0
        if (index >= 0) {
            val itemStack = items[index]
            val isShiftDown = data.modifiers and GLFW.GLFW_MOD_SHIFT != 0
            when (data.button) {
                GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                    if (!itemStack.isEmpty) {
                        val count = if (shiftDown) {
                            val mainHand = player.mainHandItem
                            if (ItemStack.isSameItemSameComponents(mainHand, itemStack)) {
                                if (mainHand.count == mainHand.maxStackSize) itemStack.maxStackSize
                                else min(mainHand.maxStackSize - mainHand.count, itemStack.count)
                            } else min(itemStack.maxStackSize, itemStack.count)
                        } else 1
                        ItemInteractivePayload.Companion.query(itemStack, count, context, -1)
                    }
                }

                GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                    if (itemStack.isEmpty) {
                        val mainHand = player.mainHandItem
                        if (!mainHand.isEmpty) {
                            val count = if (shiftDown) mainHand.count else 1
                            ItemInteractivePayload.Companion.store(mainHand, count, context, index)
                        }
                    } else {
                        val count = if (isShiftDown) {
                            val mainHand = player.mainHandItem
                            if (ItemStack.isSameItemSameComponents(mainHand, itemStack)) mainHand.count
                            else itemStack.maxStackSize
                        } else 1
                        ItemInteractivePayload.Companion.store(itemStack, count, context, -1)
                    }
                }
            }
        } else {
            if (addition && data.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                val mainHand = player.mainHandItem
                if (mainHand.isEmpty) return true
                val count = if (shiftDown) mainHand.count else 1
                ItemInteractivePayload.Companion.store(mainHand, count, context)
            }
        }

        return true
    }
}
package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.ItemInteractivePayload
import com.github.zomb_676.hologrampanel.trans.TransHandle
import com.github.zomb_676.hologrampanel.trans.TransOperation
import com.github.zomb_676.hologrampanel.trans.TransPath
import com.github.zomb_676.hologrampanel.trans.TransSource
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler
import org.lwjgl.glfw.GLFW
import kotlin.math.min

/**
 * support interactive
 */
open class InteractiveItemsElement protected constructor(
    items: List<ItemStack>,
    val source: TransSource<*>,
    val transHandle: TransHandle<*, IItemHandler>,
    addition: Boolean,
    itemEachLine: Int
) : ItemsElement(items, itemEachLine, addition) {

    companion object {
        fun <S : Any> create(
            items: List<ItemStack>,
            source: TransSource<S>,
            transHandle: TransHandle<S, IItemHandler>,
            addition: Boolean,
            itemEachLine: Int = 7
        ) = InteractiveItemsElement(items, source, transHandle, addition, itemEachLine)
    }

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

    override fun onTrigDrag(
        player: Player,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ): HologramInteractionManager.DragDataContext<*> {
        return HologramInteractionManager.DragDataContext(object : HologramInteractionManager.DragCallback<ItemStack> {
            fun getLatest() = this@InteractiveItemsElement.getCurrentUnsafe<InteractiveItemsElement>()

            override fun dragSourceStillValid(): Boolean = getLatest() != null

            override fun getDragData(): ItemStack? {
                val index = decodeIndex(mouseX, mouseY)
                if (index < 0) return null
                return items.getOrNull(index)
            }

            override fun <S : Any, H : Any> getTransInfo(): Triple<TransSource<S>, TransHandle<S, H>, TransPath<H, ItemStack>>? {
                val latest = getLatest() ?: return null
                val item = getDragData() ?: return null
                return Triple(latest.source, latest.transHandle, TransPath.Item.ByItem(item)).unsafeCast()
            }
        })
    }

    fun <S : Any> getTransData(item: ItemStack): Triple<TransSource<S>, TransHandle<S, IItemHandler>, TransPath<IItemHandler, ItemStack>> {
        return Triple(this.source, this.transHandle, TransPath.Item.ByItem(item)).unsafeCast()
    }

    override fun onDragPass(
        dragDataContext: HologramInteractionManager.DragDataContext<*>,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ) {

    }

    override fun onDragTransform(
        dragDataContext: HologramInteractionManager.DragDataContext<*>,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ) {
        if (dragDataContext.isDataOfType<ItemStack>()) {
            val data = dragDataContext.getTypedDragData<ItemStack>() ?: return
            val x = dragDataContext
                .callback.unsafeCast<HologramInteractionManager.DragCallback<ItemStack>>()
            TransOperation.create(x.getTransInfo()!!, this.getTransData(data)).sendToServer()
            dragDataContext.consumeTyped<ItemStack> { null }
        }
    }
}
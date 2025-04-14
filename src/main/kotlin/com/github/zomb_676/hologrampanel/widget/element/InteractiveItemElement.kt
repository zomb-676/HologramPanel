package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.ItemInteractivePayload
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.trans.TransHandle
import com.github.zomb_676.hologrampanel.trans.TransOperation
import com.github.zomb_676.hologrampanel.trans.TransPath
import com.github.zomb_676.hologrampanel.trans.TransSource
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler
import org.lwjgl.glfw.GLFW

open class InteractiveItemElement private constructor(
    item: ItemStack,
    val interactiveSlot: Int,
    val source: TransSource<*>,
    val transPath: TransHandle<*, IItemHandler>
) : ItemStackElement(true, item),
    HologramInteractive {

    companion object {
        fun <S : Any> create(
            item: ItemStack,
            interactiveSlot: Int,
            source: TransSource<S>,
            transPath: TransHandle<S, IItemHandler>
        ): InteractiveItemElement {
            return InteractiveItemElement(item, interactiveSlot, source, transPath)
        }
    }

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

    override fun onTrigDrag(
        player: Player,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ): HologramInteractionManager.DragDataContext<*>? {
        return HologramInteractionManager.DragDataContext(object : HologramInteractionManager.DragCallback<ItemStack> {
            fun getLatest() = this@InteractiveItemElement.getCurrentUnsafe<ItemStackElement>()

            override fun dragSourceStillValid(): Boolean = getLatest() != null
            override fun getDragData(): ItemStack? = getLatest()?.itemStack
            override fun <R : Any, H : Any> getTransInfo(): Triple<TransSource<R>, TransHandle<R, H>, TransPath<H, ItemStack>>? {
                return Triple(source, transPath, TransPath.Item.ByIndex(interactiveSlot, getDragData()!!.count)).unsafeCast()
            }
        })
    }

    override fun onDragPass(
        dragDataContext: HologramInteractionManager.DragDataContext<*>,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ) {
        if (dragDataContext.isDataOfType<ItemStack>()) {
            Minecraft.getInstance().gui.setOverlayMessage(Component.literal("trans support"), false)
        } else {
            Minecraft.getInstance().gui.setOverlayMessage(Component.literal("trans not supported"), false)
        }
    }

    fun <S : Any> getTransData(count: Int): Triple<TransSource<S>, TransHandle<S, IItemHandler>, TransPath<IItemHandler, ItemStack>> {
        return Triple(this.source, this.transPath, TransPath.Item.ByIndex(this.interactiveSlot, count)).unsafeCast()
    }

    override fun onDragTransform(
        dragDataContext: HologramInteractionManager.DragDataContext<*>,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ) {
        if (dragDataContext.isDataOfType<ItemStack>()) {
            val x = dragDataContext
                .callback.unsafeCast<HologramInteractionManager.DragCallback<ItemStack>>()
            TransOperation.create(x.getTransInfo()!!, this.getTransData(64)).sendToServer()
            dragDataContext.consumeTyped<ItemStack> { null }
        }
    }
}
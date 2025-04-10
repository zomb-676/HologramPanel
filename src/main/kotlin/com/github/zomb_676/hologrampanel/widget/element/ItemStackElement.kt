package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.TooltipType
import com.github.zomb_676.hologrampanel.util.stack
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

open class ItemStackElement(val renderDecoration: Boolean = true, val itemStack: ItemStack) : RenderElement(), HologramInteractive {

    override fun measureContentSize(style: HologramStyle): Size = style.itemStackSize().scale()

    override fun render(style: HologramStyle, partialTicks: Float) {
        if (itemStack.isEmpty) return
        style.itemFiltered(itemStack)
        if (renderDecoration) {
            style.itemDecoration(itemStack)
        }
    }

    fun smallItem(): ItemStackElement {
        this.setScale(0.5)
        return this
    }

    override fun toString(): String {
        return "ItemStack(renderDecoration=$renderDecoration, itemStack=$itemStack)"
    }

    protected val tooltipElement by lazy {
        ScreenTooltipElement(itemStack, TooltipType.SCREEN_ALWAYS_BACKGROUND)
    }

    override fun renderInteractive(
        style: HologramStyle,
        context: HologramContext,
        widgetSize: Size,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
        renderInteractiveHint: Boolean
    ) {
        if (renderInteractiveHint && !itemStack.isEmpty) {
            style.stack {
                style.move(widgetSize.width + 10, 0)
                tooltipElement.contentSize = tooltipElement.measureContentSize(style)
                tooltipElement.render(style, partialTicks)
            }
        }
    }

    override fun onTrigDrag(
        player: Player,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ): HologramInteractionManager.DragDataContext<*>? {
        return HologramInteractionManager.DragDataContext(object : HologramInteractionManager.DragCallback<ItemStack> {
            override fun processTransformRemain(remainData: ItemStack) {
                println("process transform remain")
            }
            override fun dragSourceStillValid(): Boolean = this@ItemStackElement.getCurrentUnsafe<ItemStackElement>() != null
            override fun getDragData(): ItemStack? = this@ItemStackElement.getCurrentUnsafe<ItemStackElement>()?.itemStack
        })
    }

    override fun onDragPass(
        dragDataContext: HologramInteractionManager.DragDataContext<*>,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ) {
        println("onDragPass:${dragDataContext.getDragData()}")
    }

    override fun onDragTransform(
        dragDataContext: HologramInteractionManager.DragDataContext<*>,
        context: HologramContext,
        interactiveSize: Size,
        mouseX: Int,
        mouseY: Int
    ) {
        dragDataContext.consumeTyped<ItemStack> {
            it.copyWithCount(1)
        }
    }
}

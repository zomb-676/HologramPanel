package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.TooltipType
import com.github.zomb_676.hologrampanel.util.stack
import net.minecraft.world.item.ItemStack

/**
 * render an [ItemStack] and support render item tooltip at side
 */
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
        ScreenTooltipElement(itemStack, TooltipType.SCREEN_BACKGROUND)
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
}

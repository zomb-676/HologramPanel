package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.TooltipType
import com.github.zomb_676.hologrampanel.util.stack
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.world.item.ItemStack
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

open class ItemsElement protected constructor(val items: List<ItemStack>, itemEachLine: Int, val addition: Boolean) :
    RenderElement(), HologramInteractive {
    constructor(items: List<ItemStack>, itemEachLine: Int = 7) : this(items, itemEachLine, false)

    companion object {
        const val PADDING = 1
        fun calculateLineCount(count: Int, eachLine: Int) = ceil(count.toDouble() / eachLine).toInt()
    }

    private val count = items.size + if (addition) 1 else 0
    val itemEachLine: Int
    val lineCount: Int

    init {
        val estimateLineCount = calculateLineCount(count, itemEachLine)
        if (!addition) {
            this.itemEachLine = itemEachLine
            this.lineCount = estimateLineCount
        } else {
            if (estimateLineCount * itemEachLine == count) {
                this.itemEachLine = itemEachLine
                this.lineCount = estimateLineCount
            } else {
                val addLineCount = calculateLineCount(count, itemEachLine + 1)
                val minusLineCount = calculateLineCount(count, itemEachLine - 1)
                val addDiff = abs(addLineCount * (itemEachLine + 1) - count)
                val minusDiff = abs(minusLineCount * (itemEachLine - 1) - count)
                if (addDiff < minusDiff) {
                    this.itemEachLine = itemEachLine + 1
                    this.lineCount = addLineCount
                } else {
                    this.itemEachLine = itemEachLine - 1
                    this.lineCount = minusLineCount
                }
            }
        }
    }

    override fun measureContentSize(style: HologramStyle): Size {
        val height = HologramStyle.Companion.ITEM_STACK_LENGTH * lineCount + PADDING * (lineCount - 1)
        val width = if (lineCount > 1) {
            HologramStyle.Companion.ITEM_STACK_LENGTH * itemEachLine + PADDING * (itemEachLine - 1)
        } else {
            HologramStyle.Companion.ITEM_STACK_LENGTH * count + PADDING * (count - 1)
        }
        return Size.Companion.of(width, height).scale()
    }

    override fun render(style: HologramStyle, partialTicks: Float) {
        style.font
        style.stack {
            var i = 0
            items.forEachIndexed { index, item ->
                if (i == itemEachLine) {
                    i = 0
                    style.pop()
                    style.move(0, HologramStyle.Companion.ITEM_STACK_LENGTH + PADDING)
                    style.push()
                }
                style.itemWithFilteredDecoration(item, 0, 0)
                style.move(HologramStyle.Companion.ITEM_STACK_LENGTH + PADDING, 0)
                i++
            }
        }
    }

    override fun toString(): String {
        return "Items(count=$count, items=${items.joinToString().take(30)})"
    }

    protected fun decodeIndex(mouseX: Int, mouseY: Int): Int {
        val length = HologramStyle.Companion.ITEM_STACK_LENGTH
        val padding = PADDING
        val index = mouseX / (length + padding) + max(mouseY / (length + padding), 0) * itemEachLine
        return if ((index in 0..<items.size)) {
            index
        } else {
            -1
        }
    }

    private val map: Int2ObjectMap<ScreenTooltipElement> = Int2ObjectOpenHashMap()

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
        if (renderInteractiveHint) {
            style.stack {
                style.move(widgetSize.width + 10, 0)
                val index = decodeIndex(mouseX, mouseY)
                if (index >= 0) {
                    val tooltip = map.computeIfAbsent(index) {
                        ScreenTooltipElement(items[index], TooltipType.SCREEN_ALWAYS_BACKGROUND)
                    }
                    if (tooltip.item.isEmpty) return@stack
                    tooltip.contentSize = tooltip.measureContentSize(style)
                    tooltip.render(style, partialTicks)
                }
            }
        }
    }
}

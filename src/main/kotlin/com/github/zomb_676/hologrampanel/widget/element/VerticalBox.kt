package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.DebugHelper
import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.*
import com.github.zomb_676.hologrampanel.util.packed.AlignedScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.google.common.collect.ImmutableBiMap
import kotlin.math.max

/**
 * layout element in a vertical way
 */
class VerticalBox(val elements: ImmutableBiMap<IRenderElement, String>, val context: HologramContext) : RenderElement() {
    private var baseX = 0
    private val padding = 1
    override fun measureContentSize(style: HologramStyle): Size {
        var width = 0
        var height = 0
        var calculatedSizeElement = 0
        this.elements.keys.forEach {
            it.contentSize = it.measureContentSize(style)
            val offset = it.getPositionOffset()
            if (it.hasCalculateSize()) {
                calculatedSizeElement++
                if (offset == AlignedScreenPosition.Companion.ZERO) {
                    width = max(it.contentSize.width, width)
                    height += it.contentSize.height
                } else {
                    height += it.contentSize.height + offset.y
                    if (offset.x < 0) {
                        baseX = max(baseX, -offset.x)
                    }
                    width = max(width, it.contentSize.width + offset.x)
                }
            }
        }
        height += (calculatedSizeElement - 1) * padding
        return Size.Companion.of(width, height)
    }

    override fun render(style: HologramStyle, partialTicks: Float) {
        val inMouse = style.checkMouseInSize(this.contentSize)
        if (inMouse && Config.Client.renderWidgetDebugInfo.get()) {
            style.stack {
                style.translate(0f, 0f, 100f)
                style.outline(this.contentSize, 0xff0000ff.toInt())
            }
        }
        if (baseX != 0) {
            style.move(0, baseX)
        }
        this.elements.keys.forEach { element ->
            val offset = element.getPositionOffset()
            val size = element.contentSize
            style.stackIf(offset != AlignedScreenPosition.Companion.ZERO, { style.move(offset) }) {
                style.stackIf(element.getScale() != 1.0, { style.scale(element.getScale()) }) {
                    if (inMouse && style.checkMouseInSize(size)) {
                        DebugHelper.Client.recordHoverElement(element)
                        if (Config.Client.renderWidgetDebugInfo.get()) {
                            style.stack {
                                style.translate(0f, 0f, 100f)
                                style.outline(size, 0xff0000ff.toInt())
                            }
                        }
                        if (element is HologramInteractive) {
                            HologramManager.submitInteractive(this, element, context, size, style)
                        }
                    }
                    val addLayer = element.additionLayer()
                    style.stackIf(addLayer != 0, { style.translate(0.0, 0.0, addLayer.toDouble()) }) {
                        element.render(style, partialTicks)
                    }
                }
            }
            if (element.hasCalculateSize()) {
                style.move(0, size.height + padding + offset.y)
            }
        }
    }

    override fun setReplacedBy(newCurrent: IRenderElement) {
        val old = this.current
        if (newCurrent !is VerticalBox || this.current === newCurrent) return
        val oldElements = (old as VerticalBox).elements
        val newElements = newCurrent.elements
        DynamicBuildWidget.recoveryCollapseAndNewStateForElements(oldElements, newElements)
        this.current = newCurrent
    }

    override fun setNoNewReplace() {
        super.setNoNewReplace()
        this.elements.keys.forEach(IRenderElement::setNoNewReplace)
    }
}
package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.RebuildValue
import com.github.zomb_676.hologrampanel.util.packed.AlignedScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size

/**
 * the basic interface for elements in [com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildComponentWidget.Single]
 *
 * in most case you can extend [RenderElement]
 */
interface IRenderElement : RebuildValue<IRenderElement?> {

    fun measureContentSize(style: HologramStyle): Size
    fun render(style: HologramStyle, partialTicks: Float)

    fun setScale(scale: Double): IRenderElement
    fun getScale(): Double

    fun setPositionOffset(x: Int, y: Int): IRenderElement
    fun getPositionOffset(): AlignedScreenPosition

    fun noCalculateSize(): IRenderElement
    fun hasCalculateSize(): Boolean

    fun additionLayer(): Int
    fun setAdditionLayer(layer: Int): IRenderElement

    fun setReplacedBy(newCurrent: IRenderElement)
    fun setNoNewReplace()

    var contentSize: Size

    companion object {
        fun Float.resetNan() = if (this.isNaN()) 0.0f else this

        fun shortDescription(value: Float) = when {
            value < 1e3 -> "%.2f".format(value)
            value < 1e6 -> "%.2fK".format(value / 1e3)
            value < 1e9 -> "%.2fM".format(value / 1e6)
            else -> "%.2fB".format(value / 1e9)
        }
    }
}
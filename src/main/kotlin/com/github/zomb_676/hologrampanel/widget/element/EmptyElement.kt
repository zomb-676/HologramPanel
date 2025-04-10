package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ScreenPosition
import com.github.zomb_676.hologrampanel.util.Size

data object EmptyElement : IRenderElement {
    override fun measureContentSize(style: HologramStyle): Size = Size.Companion.ZERO

    override fun render(style: HologramStyle, partialTicks: Float) {}

    override fun setScale(scale: Double): EmptyElement = this
    override fun getScale(): Double = 1.0

    override fun setPositionOffset(x: Int, y: Int): EmptyElement = this

    override fun getPositionOffset(): ScreenPosition = ScreenPosition.Companion.ZERO
    override fun noCalculateSize() = this

    override fun hasCalculateSize(): Boolean = false
    override fun additionLayer(): Int = 0

    override fun setAdditionLayer(layer: Int) = this

    override fun setReplacedBy(newCurrent: IRenderElement) {}
    override fun setNoNewReplace() {}

    override var contentSize: Size
        get() = Size.Companion.ZERO
        set(value) {}

    override fun getCurrent(): EmptyElement = this
}
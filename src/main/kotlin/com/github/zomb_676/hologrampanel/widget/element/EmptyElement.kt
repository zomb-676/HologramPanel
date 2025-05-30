package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.AlignedScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size

data object EmptyElement : IRenderElement {
    override fun measureContentSize(style: HologramStyle): Size = Size.Companion.ZERO

    override fun render(style: HologramStyle, partialTicks: Float) {}

    override fun setScale(scale: Double): EmptyElement = this
    override fun getScale(): Double = 1.0

    override fun setPositionOffset(x: Int, y: Int): EmptyElement = this

    override fun getPositionOffset(): AlignedScreenPosition = AlignedScreenPosition.Companion.ZERO
    override fun noCalculateSize() = this

    override fun hasCalculateSize(): Boolean = false
    override fun additionLayer(): Int = 0

    override fun setAdditionLayer(layer: Int) = this

    override fun setReplacedBy(newCurrent: IRenderElement) {}
    override fun setNoNewReplace() {}

    override fun setLimitHeight(limitHeight: Int) {}
    override fun getLimitHeight(): Int = 0
    override fun isLimitHeight(): Boolean = false

    override var contentSize: Size
        get() = Size.Companion.ZERO
        set(value) {}

    override fun getCurrent(): EmptyElement = this

    override fun toString(): String {
        return "EmptyRenderElement"
    }


}
package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.util.packed.AlignedScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.unsafeCast
import kotlin.math.floor

abstract class RenderElement : IRenderElement {
    final override var contentSize: Size = Size.Companion.ZERO

    private var scale: Double = 1.0
        @JvmName("privateScaleSet") set(value) {
            require(value > 0)
            field = value
        }
    private var hasCalculateSize = true

    private var additionLayer = 0

    private var positionOffset: AlignedScreenPosition = AlignedScreenPosition.Companion.ZERO

    @JvmField
    protected var current: RenderElement? = this

    private var limitHeight: Int = 0

    protected fun Size.scale(): Size {
        if (scale == 1.0) {
            return this
        } else {
            val w = floor(this.width * scale).toInt()
            val h = floor(this.height * scale).toInt()
            return Size.Companion.of(w, h)
        }
    }

    final override fun getPositionOffset(): AlignedScreenPosition = this.positionOffset

    final override fun setPositionOffset(x: Int, y: Int): RenderElement {
        this.positionOffset = AlignedScreenPosition.Companion.of(x, y)
        return this
    }

    final override fun getScale(): Double = scale

    final override fun setScale(scale: Double): RenderElement {
        this.scale = scale
        return this
    }

    final override fun hasCalculateSize(): Boolean = hasCalculateSize

    final override fun noCalculateSize(): RenderElement {
        this.hasCalculateSize = false
        return this
    }

    final override fun additionLayer(): Int = additionLayer

    final override fun setAdditionLayer(layer: Int): RenderElement {
        this.additionLayer = layer
        return this
    }

    final override fun hasAdditionLayer(): Boolean = super.hasAdditionLayer()

    final override fun getCurrent(): RenderElement? {
        var current: RenderElement = current ?: return null
        while (current != current.current) {
            current = current.current ?: run {
                this.current = null
                return null
            }
        }
        this.current = current
        return current
    }

    override fun setReplacedBy(newCurrent: IRenderElement) {
        this.current = newCurrent.unsafeCast()
    }

    override fun setNoNewReplace() {
        this.current = null
    }

    final override fun setLimitHeight(limitHeight: Int) {
        require(limitHeight >= 0)
        this.limitHeight = limitHeight
    }

    final override fun getLimitHeight(): Int = this.limitHeight
    final override fun isLimitHeight(): Boolean = this.limitHeight > 0
    final override fun isLimitHeight(value: Int): Boolean = super.isLimitHeight(value)
}
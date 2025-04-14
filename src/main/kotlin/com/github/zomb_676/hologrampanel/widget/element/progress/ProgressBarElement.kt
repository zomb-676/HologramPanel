package com.github.zomb_676.hologrampanel.widget.element.progress

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.element.RenderElement
import net.minecraft.network.chat.Component
import kotlin.math.floor

abstract class ProgressBarElement(val progress: ProgressData, var barWidth: Float = 98f) : RenderElement() {

    override fun measureContentSize(
        style: HologramStyle
    ): Size {
        //+2 is the outline part of the progress
        return Size.Companion.of(floor(barWidth + 2).toInt(), style.font.lineHeight + 2).scale()
    }

    override fun render(style: HologramStyle, partialTicks: Float) {
        if (this.requireOutlineDecorate()) {
            style.outline(this.contentSize, style.contextColor)
        }

        val percent = progress.percent
        style.stack {
            if (this.requireOutlineDecorate()) {
                style.move(1, 1)
            }
            val left: Float
            val right: Float
            val height: Float = if (this.requireOutlineDecorate()) {
                this.contentSize.height - 2
            } else {
                this.contentSize.height
            }.toFloat()
            if (progress.LTR) {
                left = 0.0f
                right = barWidth * percent
            } else {
                left = (1.0f - percent) * barWidth
                right = barWidth.toFloat()
            }
            this.fillBar(style, left, right, height, percent)
        }

        val description = getDescription(percent)
        val width = style.measureString(description).width
        style.stack {
            style.translate(0.0, 0.0, 1.0)
            style.drawString(description, (this.contentSize.width - width) / 2, 2)
        }
    }

    abstract fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float)
    open fun getDescription(percent: Float): Component = Component.literal("%.1f%%".format(percent * 100))

    open fun requireOutlineDecorate(): Boolean = false

    override fun toString(): String {
        return "ProgressBar(progress=$progress, barWidth=$barWidth)"
    }
}
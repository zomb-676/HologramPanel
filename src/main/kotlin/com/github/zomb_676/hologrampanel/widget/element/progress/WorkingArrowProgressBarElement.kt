package com.github.zomb_676.hologrampanel.widget.element.progress

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.renderstate.FloatColoredQuadrilateralRenderState.Companion.fillQuad
import com.github.zomb_676.hologrampanel.util.renderstate.FloatColoredRectangleRenderState.Companion.fill
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement.Companion.resetNan

class WorkingArrowProgressBarElement(progress: ProgressData, barWidth: Float = 15f) :
    ProgressBarElement(progress, barWidth) {

    override fun render(style: HologramStyle, partialTicks: Float) {
        val percent = progress.percent.resetNan()
        style.stack {
            style.move(1, 1)
            val height = (this.contentSize.height - 2).toFloat()
            if (progress.LTR) {
                this.fillBar(style, 0.0f, barWidth * percent, height, percent)
            } else {
                this.fillBar(style, (1.0f - percent) * barWidth, barWidth, height, percent)
            }
        }
    }

    companion object {
        const val AXIAL_HALF_WIDTH = 1.75f
        const val BASE_COLOR = 0xff5b5b5b.toInt()
        const val FILL_COLOR = -1
    }

    override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
        val width = this.barWidth

        val halfHeight = height / 2
        val cuttingPointX = width - (height / 2.0f)
        //draw base
        style.guiGraphics.fill(left,  halfHeight - AXIAL_HALF_WIDTH, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, BASE_COLOR)

        style.guiGraphics.fillQuad(
            width, height / 2,
            cuttingPointX, 0.0f,
            cuttingPointX, height,
            cuttingPointX, height,
            BASE_COLOR
        )

        //draw fill
        if (right <= cuttingPointX) {
            style.guiGraphics.fill(left, halfHeight - AXIAL_HALF_WIDTH, right, halfHeight + AXIAL_HALF_WIDTH, FILL_COLOR)
        } else {
            style.guiGraphics.fill(left, halfHeight - AXIAL_HALF_WIDTH, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, FILL_COLOR)

            val remain = width - right
            style.guiGraphics.fillQuad(
                cuttingPointX, 0.0f,
                cuttingPointX, height,
                right, halfHeight + remain,
                right, halfHeight - remain,
                FILL_COLOR
            )
        }
    }

    override fun toString(): String {
        return "(style=Arrow,${super.toString()})"
    }
}
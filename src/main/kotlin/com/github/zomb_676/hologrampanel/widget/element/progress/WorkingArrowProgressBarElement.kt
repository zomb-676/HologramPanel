package com.github.zomb_676.hologrampanel.widget.element.progress

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement.Companion.resetNan
import net.minecraft.client.renderer.RenderType

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
        val left = left.toFloat()
        val right = right.toFloat()
        val height = height.toFloat()
        style.guiGraphics.flush()

        val buffer = style.guiGraphics.bufferSource
        val consumer = buffer.getBuffer(RenderType.gui())
        val width = this.barWidth

        val pose = style.poseMatrix()

        val halfHeight = height / 2
        val cuttingPointX = width - (height / 2.0f)
        //draw base
        consumer.addVertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)
        consumer.addVertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)
        consumer.addVertex(pose, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)
        consumer.addVertex(pose, cuttingPointX, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)

        consumer.addVertex(pose, width, height / 2, 0.0f).setColor(BASE_COLOR)
        consumer.addVertex(pose, cuttingPointX, 0.0f, 0.0f).setColor(BASE_COLOR)
        consumer.addVertex(pose, cuttingPointX, height, 0.0f).setColor(BASE_COLOR)
        //another for quad
        consumer.addVertex(pose, cuttingPointX, height, 0.0f).setColor(BASE_COLOR)

        //draw fill
        if (right <= cuttingPointX) {
            consumer.addVertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
            consumer.addVertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
            consumer.addVertex(pose, right, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
            consumer.addVertex(pose, right, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
        } else {
            consumer.addVertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
            consumer.addVertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
            consumer.addVertex(pose, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
            consumer.addVertex(pose, cuttingPointX, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)

            consumer.addVertex(pose, cuttingPointX, 0.0f, 0.0f).setColor(FILL_COLOR)
            consumer.addVertex(pose, cuttingPointX, height, 0.0f).setColor(FILL_COLOR)
            val remain = width - right
            consumer.addVertex(pose, right, halfHeight + remain, 0.0f).setColor(FILL_COLOR)
            consumer.addVertex(pose, right, halfHeight - remain, 0.0f).setColor(FILL_COLOR)
        }
    }

    override fun toString(): String {
        return "(style=Arrow,${super.toString()})"
    }
}
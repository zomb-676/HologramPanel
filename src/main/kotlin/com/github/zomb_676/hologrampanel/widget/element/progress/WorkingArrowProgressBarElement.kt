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

        val buffer = style.guiGraphics.bufferSource()
        val consumer = buffer.getBuffer(RenderType.gui())
        val width = this.barWidth

        val pose = style.poseMatrix()

        val halfHeight = height / 2
        val cuttingPointX = width - (height / 2.0f)
        //draw base
        consumer.vertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).color(BASE_COLOR).endVertex()
        consumer.vertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).color(BASE_COLOR).endVertex()
        consumer.vertex(pose, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, 0.0f).color(BASE_COLOR).endVertex()
        consumer.vertex(pose, cuttingPointX, halfHeight - AXIAL_HALF_WIDTH, 0.0f).color(BASE_COLOR).endVertex()

        consumer.vertex(pose, width, height / 2, 0.0f).color(BASE_COLOR).endVertex()
        consumer.vertex(pose, cuttingPointX, 0.0f, 0.0f).color(BASE_COLOR).endVertex()
        consumer.vertex(pose, cuttingPointX, height, 0.0f).color(BASE_COLOR).endVertex()
        //another for quad
        consumer.vertex(pose, cuttingPointX, height, 0.0f).color(BASE_COLOR).endVertex()

        //draw fill
        if (right <= cuttingPointX) {
            consumer.vertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).color(FILL_COLOR).endVertex()
            consumer.vertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).color(FILL_COLOR).endVertex()
            consumer.vertex(pose, right, halfHeight + AXIAL_HALF_WIDTH, 0.0f).color(FILL_COLOR).endVertex()
            consumer.vertex(pose, right, halfHeight - AXIAL_HALF_WIDTH, 0.0f).color(FILL_COLOR).endVertex()
        } else {
            consumer.vertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).color(FILL_COLOR).endVertex()
            consumer.vertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).color(FILL_COLOR).endVertex()
            consumer.vertex(pose, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, 0.0f).color(FILL_COLOR).endVertex()
            consumer.vertex(pose, cuttingPointX, halfHeight - AXIAL_HALF_WIDTH, 0.0f).color(FILL_COLOR).endVertex()

            consumer.vertex(pose, cuttingPointX, 0.0f, 0.0f).color(FILL_COLOR).endVertex()
            consumer.vertex(pose, cuttingPointX, height, 0.0f).color(FILL_COLOR).endVertex()
            val remain = width - right
            consumer.vertex(pose, right, halfHeight + remain, 0.0f).color(FILL_COLOR).endVertex()
            consumer.vertex(pose, right, halfHeight - remain, 0.0f).color(FILL_COLOR).endVertex()
        }
    }

    override fun toString(): String {
        return "(style=Arrow,${super.toString()})"
    }
}
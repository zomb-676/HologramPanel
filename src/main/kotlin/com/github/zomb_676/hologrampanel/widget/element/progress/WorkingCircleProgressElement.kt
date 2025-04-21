package com.github.zomb_676.hologrampanel.widget.element.progress

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement.Companion.resetNan
import com.github.zomb_676.hologrampanel.widget.element.RenderElement
import kotlin.math.floor

class WorkingCircleProgressElement(val progress: ProgressData, val outRadius: Float, val inRadius: Float) :
    RenderElement() {
    override fun measureContentSize(style: HologramStyle): Size {
        return Size.Companion.of(floor(outRadius * 2).toInt())
    }

    companion object {
        const val BASE_COLOR = 0xff88abdc.toInt()
        const val FILL_COLOR = 0xff4786da.toInt()
    }

    override fun render(style: HologramStyle, partialTicks: Float) {
        val percent = progress.percent.resetNan()

        style.stack {
            val move = this.contentSize.width / 2f
            style.translate(move, move)
            val end = percent * Math.PI * 2
            if (inRadius > 0.01f) {
                style.drawTorus(inRadius, outRadius, FILL_COLOR, beginRadian = Math.PI, endRadian = Math.PI - end)
            } else {
                style.drawCycle(outRadius, BASE_COLOR, beginRadian = Math.PI * 2, endRadian = 0.0)
                style.drawCycle(outRadius, FILL_COLOR, beginRadian = Math.PI, endRadian = Math.PI - end)
            }
        }
    }

    override fun toString(): String {
        return "(style=Circle, progress:$progress)"
    }
}
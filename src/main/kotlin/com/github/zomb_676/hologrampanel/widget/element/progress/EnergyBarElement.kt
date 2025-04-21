package com.github.zomb_676.hologrampanel.widget.element.progress

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement.Companion.shortDescription
import net.minecraft.network.chat.Component

class EnergyBarElement(progress: ProgressData, barWidth: Float = 60f) : ProgressBarElement(progress, barWidth) {
    override fun requireOutlineDecorate(): Boolean = true

    override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
        style.fill(left, 0f, right, height, 0xffFF6B6B.toInt())
        if (progress.LTR) {
            style.fill(right, 0f, barWidth, height, 0xafDDDDDD.toInt())
        } else {
            style.fill(0f, left, barWidth, height, 0xafDDDDDD.toInt())
        }
    }

    override fun getDescription(percent: Float): Component {
        val current = if (progress.progressCurrent < 1000) {
            "${progress.progressCurrent}"
        } else {
            shortDescription(progress.progressCurrent.toFloat())
        }
        val f = if (progress.progressCurrent == progress.progressMax) {
            current
        } else {
            val max = if (progress.progressMax < 1000) {
                "${progress.progressMax}"
            } else {
                shortDescription(progress.progressMax.toFloat())
            }
            "$current/$max"
        }
        return Component.literal("").append(f).append("FE")
    }

    override fun toString(): String {
        return "EnergyBar,${super.toString()}"
    }
}
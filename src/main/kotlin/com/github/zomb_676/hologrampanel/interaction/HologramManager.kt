package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.interaction.InteractionCommand.Exact
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.JomlMath
import com.github.zomb_676.hologrampanel.util.mainCamera
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidget
import com.mojang.blaze3d.platform.Window
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import kotlin.math.sqrt

object HologramManager {
    private val widgets = mutableMapOf<Any, HologramWidget>()
    private val states = mutableMapOf<HologramWidget, HologramState>()
    private var lookingWidget: HologramState? = null

    fun clearHologram() {
        this.widgets.clear()
        this.states.clear()
        this.lookingWidget = null
    }

    private var needArrange = false

    fun tryAddWidget(widget: HologramWidget, traceSource: BlockPos) {
        if (!widgets.containsKey(traceSource)) {
            widgets[traceSource] = widget
            states[widget] = HologramState(widget, traceSource)
            this.needArrange = true

            widget.onAdd()
            if (widget is HologramComponentWidget<*>) {
                widget.collectServerDataRequired()
            }
        }
    }

    internal fun render(guiGraphics: GuiGraphics, partialTicks: Float) {
        val res = RayTraceHelper.findTarget(32, partialTicks)
        if (res != null && !widgets.containsKey(res.first)) {
            val widget = RayTraceHelper.createHologramWidget(res.second)
            this.tryAddWidget(widget, res.first)
        }

        if (needArrange) {
            needArrange = false
            //todo do arrange here
        }

        val style: HologramStyle = HologramStyle.DefaultStyle(guiGraphics)
        states.values.forEach { state ->
            val (widget, pos) = state
            val widgetSize = state.measure(HologramWidget.DisplayType.FOCUSED, style)

            style.stack {
                if (!state.viewVectorDegreeCheckPass()) return@stack

                val screenPos = state.updateScreenPosition().equivalentSmooth(style)
                style.move(screenPos.x, screenPos.y)
                val distance = sqrt(mainCamera().position.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))

                fun calculateScale(distance: Double, start: Double, end: Double): Double = when {
                    distance <= start -> 1.0
                    distance >= end -> 0.0
                    else -> JomlMath.clamp(0.0, 1.0, 1.0 - (distance - start) / (end - start))
                }

                val scale = calculateScale(distance, 1.0, 8.0)

                state.displayed = if (scale * widgetSize.width < 5 || scale * widgetSize.height < 3) {
                    false
                } else {
                    state.setDisplaySize(scale)
                    state.displayAreaInScreen()
                }
                if (!state.displayed) return@stack


                style.scale(scale, scale)
                style.translate(-widgetSize.width / 2.0, -widgetSize.height / 2.0)
                style.fill(0, 0, widgetSize.width, widgetSize.height, 0x7fffffff)
                widget.render(state, style, partialTicks)
            }
        }
        this.updateLookingAt()

        this.renderHologramStateTip(style, InteractionModeManager.getSelectedHologram(), style.contextColor, 5)
        this.renderHologramStateTip(style, getLookingHologram(), 0xff_00a2e8.toInt(), 8)
        this.renderHologramStateTip(style, InteractionModeManager.getFindCandidateHologram(), 0xff_efe4b0.toInt(), 11)
    }

    private fun renderHologramStateTip(style: HologramStyle, target: HologramState?, color: Int, baseOffset: Int) {
        val target = target ?: return
        if (!target.displayed) return
        style.stack {
            val screenPos = target.centerScreenPos.equivalentSmooth(style)

            val displayWidth = target.size.width * target.displayScale
            val displayHeight = target.size.height * target.displayScale

            val left = (screenPos.x - displayWidth / 2.0) - (baseOffset * target.displayScale)
            val right = (screenPos.x + displayWidth / 2.0) + (baseOffset * target.displayScale)
            val up = (screenPos.y - displayHeight / 2.0) - (baseOffset * target.displayScale)
            val down = (screenPos.y + displayHeight / 2.0) + (baseOffset * target.displayScale)

            val horizontalLength = (displayWidth * 0.2).toInt()
            val verticalLength = (displayHeight * 0.2).toInt()

            fun drawVerticalLine(up: Double, down: Double, x: Double) {
                style.stack {
                    val fixX = x.toInt()
                    val fixY = up.toInt()
                    style.translate(x - fixX, up - fixY)
                    style.drawVerticalLine(fixY, down.toInt(), fixX, color)
                }
            }

            fun drawHorizontalLine(left: Double, right: Double, y: Double) {
                style.stack {
                    val fixX = left.toInt()
                    val fixY = y.toInt()
                    style.translate(left - fixX, y - fixY)
                    style.drawHorizontalLine(fixX, right.toInt(), fixY, color)
                }
            }

            drawVerticalLine(up, up + verticalLength, left)
            drawVerticalLine(down - verticalLength, down, left)

            drawHorizontalLine(left, left + horizontalLength, up)
            drawHorizontalLine(left, left + horizontalLength, down)

            drawVerticalLine(up, up + verticalLength, right)
            drawVerticalLine(down - verticalLength, down, right)

            drawHorizontalLine(right - horizontalLength, right, up)
            drawHorizontalLine(right - horizontalLength, right, down)
        }
    }

    private fun updateLookingAt() {
        val window: Window = Minecraft.getInstance().window
        val checkX = window.guiScaledWidth / 2
        val checkY = window.guiScaledHeight / 2
        this.lookingWidget = this.states.values
            .asSequence()
            .filter { it.displayed }
            .firstOrNull { state ->
                val size = state.displaySize
                val position = state.centerScreenPos
                val left = position.screenX - size.width / 2
                if (left > checkX) return@firstOrNull false
                val right = position.screenX + size.width / 2
                if (right < checkX) return@firstOrNull false
                val up = position.screenY - size.height / 2
                if (up > checkY) return@firstOrNull false
                val down = position.screenY + size.height / 2
                return@firstOrNull down > checkY
            }
    }

    fun getLookingHologram(): HologramState? {
        return this.lookingWidget
    }

    fun getInteractionCandidate(): HologramState? {
        return InteractionModeManager.getSelectedHologram() ?: this.getLookingHologram()
    }

    fun getSubsequentDisplayedCandidate(state: HologramState?, exact: Exact.SelectHologram): HologramState? {
        return when (exact) {
            Exact.SelectHologram.SELECT_HOLOGRAM -> getLookingHologram()
            Exact.SelectHologram.SWITCH_HOLOGRAM_UP -> null
            Exact.SelectHologram.SWITCH_HOLOGRAM_RIGHT -> null
            Exact.SelectHologram.SWITCH_HOLOGRAM_DOWN -> null
            Exact.SelectHologram.SWITCH_HOLOGRAM_LEFT -> null
            Exact.SelectHologram.SWITCH_HOLOGRAM_BEFORE -> if (state == null) {
                this.states.values.first { it.displayed }
            } else {
                val values = this.states.values.toList()
                val index = values.indexOf(state)
                run {
                    if (index < 0) return@run null
                    if (index > 0) {
                        for (i in (index - 1) downTo 0) {
                            val next = values[i]
                            if (next.displayed) return@run next
                        }
                    }
                    for (i in (values.size - 1) downTo (index + 1)) {
                        val next = values[i]
                        if (next.displayed) return@run next
                    }
                    return@run null
                }
            }


            Exact.SelectHologram.SWITCH_HOLOGRAM_NEXT -> if (state == null) {
                this.states.values.first { it.displayed }
            } else {
                val values = this.states.values.toList()
                val index = values.indexOf(state)
                run {
                    if (index >= 0) {
                        for (i in (index + 1)..<values.size) {
                            val next = values[i]
                            if (next.displayed) return@run next
                        }
                        for (i in 0..<index) {
                            val next = values[i]
                            if (next.displayed) return@run next
                        }
                        null
                    } else null
                }
            }

            Exact.SelectHologram.UNSELECT -> null
        } ?: state
    }

    fun remove(widget: HologramWidget) {
        if (this.widgets.remove(widget) != null) {
            this.states.remove(widget)
            if (this.lookingWidget?.widget == widget) {
                this.lookingWidget = null
            }
            InteractionModeManager.onWidgetRemoved(widget)
        }
    }
}
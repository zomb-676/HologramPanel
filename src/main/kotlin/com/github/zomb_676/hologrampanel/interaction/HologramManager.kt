package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.DebugHelper
import com.github.zomb_676.hologrampanel.api.HologramHolder
import com.github.zomb_676.hologrampanel.api.HologramTicket
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.InteractiveEntry
import com.github.zomb_676.hologrampanel.util.JomlMath
import com.github.zomb_676.hologrampanel.util.profiler
import com.github.zomb_676.hologrampanel.util.profilerStack
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.mojang.blaze3d.platform.Window
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

object HologramManager {
    private val widgets = mutableMapOf<Any, HologramWidget>()
    internal val states = mutableMapOf<HologramWidget, HologramRenderState>()
    private var lookingWidget: HologramRenderState? = null
    private var interactiveTarget: InteractiveEntry? = null
    private var collapseTarget: HologramWidgetComponent.Group<*>? = null

    fun clearAllHologram() {
        while (states.isNotEmpty()) {
            states.entries.first().key.closeWidget()
        }
    }

    private var needArrange = false

    fun checkIdentityExist(any: Any): Boolean {
        return widgets.containsKey(any)
    }

    fun tryAddWidget(
        widget: HologramWidget,
        context: HologramContext,
        displayType: DisplayType,
        ticket: List<HologramTicket<*>>,
    ): HologramRenderState? {
        if (!widgets.containsKey(context.getIdentityObject())) {
            widgets[context.getIdentityObject()] = widget
            val state = HologramRenderState(widget, context, displayType, ticket)
            states[widget] = state
            this.needArrange = true

            widget.onAdd()

            return state
        }
        return null
    }

    internal fun render(guiGraphics: GuiGraphics, partialTicks: Float) = profilerStack("hologram_panel_render") {
        val context = RayTraceHelper.findTarget(32.0, partialTicks)
        if (context != null && !widgets.containsKey(context.getIdentityObject())) {
            val widget = RayTraceHelper.createHologramWidget(context, DisplayType.NORMAL)
            if (widget != null) {
                this.tryAddWidget(widget, context, DisplayType.NORMAL, listOf(HologramTicket.ByTickAfterNotSee(80)))
            }
        }

        if (needArrange) {
            needArrange = false
            //todo do arrange here
        }

        profiler.push("render_hologram")
        this.interactiveTarget = null
        this.collapseTarget = null
        val scaleValue = Config.Client.globalHologramScale.get() / run {
            val window = Minecraft.getInstance().window
            //not use the guiScale current, use auto gui scale to keep hologram size
            window.guiScale / window.calculateScale(0, Minecraft.getInstance().isEnforceUnicode)
        }
        DebugHelper.Client.clearRenderRelatedInfo()
        val style: HologramStyle = HologramStyle.DefaultStyle(guiGraphics)
        states.forEach { (widget, state) ->
            if (Config.Client.skipHologramIfEmpty.get() && !widget.hasNoneOrdinaryContent()) {
                state.displayed = false
                return@forEach
            }
            val displayType = state.displayType
            val widgetSize = state.measure(displayType, style)

            if (!state.viewVectorDegreeCheckPass(partialTicks)) return@forEach
            style.stack {

                val screenPos = state.updateScreenPosition(partialTicks).equivalentSmooth(style)
                style.move(screenPos.x, screenPos.y)
                val distance = state.distanceToCamera(partialTicks)

                fun calculateScale(distance: Double, start: Double, end: Double): Double = when {
                    distance <= start -> 1.0
                    distance >= end -> 0.0
                    else -> JomlMath.clamp(0.0, 1.0, 1.0 - (distance - start) / (end - start))
                }

                val scale = calculateScale(
                    distance, Config.Client.renderMinDistance.get(),
                    Config.Client.renderMaxDistance.get()
                ) * scaleValue

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

                val interactiveSet = this.getInteractiveTarget() != null
                style.stack {
                    widget.render(state, style, displayType, partialTicks)
                }
                val currentInteractive = this.getInteractiveTarget()
                if (!interactiveSet && currentInteractive != null) {
                    currentInteractive.renderInteractive(style, state.size, partialTicks)
                }
            }
        }
        this.updateLookingAt()

//        this.renderHologramStateTip(style, InteractionModeManager.getSelectedHologram(), style.contextColor, 5)
        this.renderHologramStateTip(style, getLookingHologram(), 0xff_00a2e8.toInt(), 8)
//        this.renderHologramStateTip(style, InteractionModeManager.getFindCandidateHologram(), 0xff_efe4b0.toInt(), 11)
        profiler.pop()
    }

    private fun renderHologramStateTip(
        style: HologramStyle,
        target: HologramRenderState?,
        color: Int,
        baseOffset: Int
    ) {
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

    fun getLookingHologram(): HologramRenderState? {
        return this.lookingWidget
    }

    /**
     * this will do all the staffs if a widget should be removed from client
     */
    fun remove(widget: HologramWidget) {
        val state = this.states.remove(widget)
        if (state != null) {
            state.removed = true
            val context = state.context
            this.widgets.remove(context.getIdentityObject())
            if (this.lookingWidget?.widget == widget) {
                this.lookingWidget = null
            }
            if (context is EntityHologramContext) {
                (context.getEntity() as HologramHolder).setWidget(null)
            }

            if (widget is DynamicBuildWidget<*>) {
                DataQueryManager.Client.closeForWidget(widget)
            }

            DebugHelper.Client.recordRemove(state)
        }
    }

    fun clientTick() {
        val forRemoved = ArrayList<HologramRenderState>(0)
        this.states.forEach { (widget, state) ->
            val context = state.context
            if (state.stillValid()) {
                val remember = context.getRememberData()
                remember.tickMimicClientUpdate()
                remember.tickClientValueUpdate()
                if (remember.needUpdate()) {
                    if (widget is DynamicBuildWidget<*>) {
                        widget.updateComponent(state.displayType)
                    }
                }
            } else {
                forRemoved.add(state)
            }
        }
        if (forRemoved.isNotEmpty()) {
            forRemoved.forEach { it.widget.closeWidget() }
        }
    }

    fun queryHologramState(widget: HologramWidget?): HologramRenderState? = states[widget]

    fun widgetCount(): Int = states.size

    fun submitInteractive(interactiveEntry: InteractiveEntry) {
        this.interactiveTarget = interactiveEntry
    }

    fun getInteractiveTarget(): InteractiveEntry? = this.interactiveTarget

    fun getCollapseTarget() = this.collapseTarget

    fun setCollapseTarget(group: HologramWidgetComponent.Group<*>) {
        this.collapseTarget = group
    }

    fun trySwitchWidgetCollapse() {
        this.collapseTarget?.switchCollapse()
    }
}
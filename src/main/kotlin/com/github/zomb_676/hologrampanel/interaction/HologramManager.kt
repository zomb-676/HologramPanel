package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.DebugHelper
import com.github.zomb_676.hologrampanel.api.HologramHolder
import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.api.HologramTicket
import com.github.zomb_676.hologrampanel.interaction.HologramManager.collapseTarget
import com.github.zomb_676.hologrampanel.interaction.HologramManager.interactiveTarget
import com.github.zomb_676.hologrampanel.interaction.HologramManager.lookingWidget
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.render.LinkLineRender
import com.github.zomb_676.hologrampanel.util.*
import com.github.zomb_676.hologrampanel.util.packed.AlignedScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement
import com.mojang.blaze3d.platform.Window
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.CoreShaders
import org.joml.Matrix4f
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3f

object HologramManager {
    /**
     * mapping from [HologramContext.getIdentityObject] to [HologramWidget]
     */
    private val widgets = mutableMapOf<Any, HologramWidget>()

    /**
     * mapping from [HologramWidget] to [HologramRenderState]
     */
    internal val states = mutableMapOf<HologramWidget, HologramRenderState>()

    /**
     * the widget that is looking
     */
    private var lookingWidget: HologramRenderState? = null

    /**
     * the entry that is interacted
     */
    private var interactiveTarget: InteractiveEntry? = null

    /**
     * the entry that should response collapse operation
     */
    private var collapseTarget: HologramWidgetComponent.Group<*>? = null

    private var screenPingHolograms: MutableList<HologramRenderState> = mutableListOf()

    fun clearAllHologram() {
        while (states.isNotEmpty()) {
            states.entries.first().key.closeWidget()
        }
    }

    private var needArrange = false

    /**
     * check via [HologramContext.getIdentityObject]
     */
    fun checkIdentityExist(any: Any): Boolean {
        return widgets.containsKey(any)
    }

    /**
     * @return not null if added success
     *
     * check if [HologramContext.getIdentityObject] exists
     * update internal state and warp it as [HologramRenderState]
     */
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

    /**
     * trig Hologram by [HologramTicket.ByTickAfterNotSee]
     *
     * update [HologramRenderState], [lookingWidget], [interactiveTarget], [collapseTarget] and do render
     */
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
            //measure size
            profiler.push("measure")
            val widgetSize = state.measure(displayType, style)
            profiler.pop()

            //clip hologram at back
            if (!state.viewVectorDegreeCheckPass(partialTicks)) {
                state.displayed = false
                return@forEach
            }
            style.stack {
                //calculate screen position by world position
                val screenPos = state.updateRenderScreenPosition(partialTicks).equivalentSmooth(style)
                style.move(screenPos.x, screenPos.y)
                //change scale according to distance
                val scale: Double = scaleValue * if (state.locate is LocateType.World) {
                    val distance = state.distanceToCamera(partialTicks)
                    calculateScale(
                        distance, Config.Client.renderMinDistance.get(),
                        Config.Client.renderMaxDistance.get()
                    )
                } else 1.0

                //skip tp small widget
                if (scale * widgetSize.width < 5 || scale * widgetSize.height < 3) {
                    state.displayed = false
                    return@stack
                }
                state.setDisplayScale(scale)
                style.scale(scale, scale)

                //anchored by hologram's left-up
                if (state.locate is LocateType.World) {
                    style.translate(-widgetSize.width / 2.0, -widgetSize.height / 2.0)
                }

                //check if any part in screen
                state.displayed = state.displayAreaInScreen(style.poseMatrix())
                if (!state.displayed) return@stack

                //hologram background
                style.drawFullyBackground(widgetSize)

                //check if interact submitted during render
                val interactiveSet = this.getInteractiveTarget() != null
                style.stack {
                    //do actual render
                    widget.render(state, style, displayType, partialTicks)
                }
                val currentInteractive = this.getInteractiveTarget()
                if (!interactiveSet && currentInteractive != null) {
                    currentInteractive.renderInteractive(style, state.size, partialTicks)
                }
            }
        }
        this.updateLookingAt()
        this.arrangeScreenPingWidget(partialTicks)

        if (Config.Style.renderLookIndicator.get()) {
            val distance = Config.Style.lookIndicatorDistance.get()
            val percent = Config.Style.lookIndicatorPercent.get()
            this.renderHologramStateTip(style, getLookingHologram(), 0xff_00a2e8.toInt(), distance, percent)
        }

        HologramInteractionManager.renderTick()
        this.renderPingScreenPrompt(style, partialTicks)

        profiler.pop()
    }

    private fun calculateScale(distance: Double, start: Double, end: Double): Double = when {
        distance <= start -> 1.0
        distance >= end -> 0.0
        else -> JomlMath.clamp(0.0, 1.0, 1.0 - (distance - start) / (end - start))
    }

    /**
     * the tip line that indicates some HologramState that should be highlighted
     *
     * @param color the highlight line color
     * @param baseOffset distance between Hologram and the highlight line
     */
    private fun renderHologramStateTip(
        style: HologramStyle,
        target: HologramRenderState?,
        color: Int,
        baseOffset: Int,
        percent: Double
    ) {
        val target = target ?: return
        if (!target.displayed) return
        style.stack {
            val screenPos = target.centerScreenPos

            val displayWidth = target.displaySize.width
            val displayHeight = target.displaySize.height

            val scaledOffset = baseOffset * target.displayScale
            val left = (screenPos.x - displayWidth / 2.0) - scaledOffset
            val right = (screenPos.x + displayWidth / 2.0) + scaledOffset
            val up = (screenPos.y - displayHeight / 2.0) - scaledOffset
            val down = (screenPos.y + displayHeight / 2.0) + scaledOffset

            val horizontalLength = (displayWidth * percent).toInt()
            val verticalLength = (displayHeight * percent).toInt()

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

    /**
     * find the widget that is looking
     */
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
                val left = position.x - size.width / 2
                if (left > checkX) return@firstOrNull false
                val right = position.x + size.width / 2
                if (right < checkX) return@firstOrNull false
                val up = position.y - size.height / 2
                if (up > checkY) return@firstOrNull false
                val down = position.y + size.height / 2
                return@firstOrNull down > checkY
            }
    }

    fun getLookingHologram(): HologramRenderState? {
        return this.lookingWidget
    }

    /**
     * this will do all the staffs if a widget should be removed from the client side
     */
    fun remove(widget: HologramWidget) {
        val state = this.states.remove(widget)
        if (state != null) {
            state.removed = true
            val context = state.context
            this.widgets.remove(context.getIdentityObject())
            this.screenPingHolograms.remove(state)
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

    /**
     * the [HologramRenderState] by [HologramWidget]
     */
    fun queryHologramState(widget: HologramWidget?): HologramRenderState? = states[widget]

    /**
     * the widget count that is in memory, used for statistics
     */
    fun widgetCount(): Int = states.size

    /**
     * @param size the size the interactive take
     * @param hologramStyle query actual position from its matrix
     */
    fun <T> submitInteractive(
        container: Any,
        interactive: T,
        context: HologramContext,
        size: Size,
        hologramStyle: HologramStyle
    ) where T : HologramInteractive, T : RebuildValue<IRenderElement?> {
        this.interactiveTarget = InteractiveEntry(container, interactive, context, size, Matrix4f(hologramStyle.poseMatrix()), this.interactiveTarget)
    }

    fun getInteractiveTarget(): InteractiveEntry? = this.interactiveTarget

    fun getCollapseTarget() = this.collapseTarget

    fun setCollapseTarget(group: HologramWidgetComponent.Group<*>) {
        this.collapseTarget = group
    }

    fun trySwitchWidgetCollapse() {
        this.collapseTarget?.switchCollapse()
    }

    fun tryPingLookingScreen() {
        val looking = getLookingHologram() ?: return
        looking.locate = LocateType.Screen(Vector2f(30f, 30f))
        this.screenPingHolograms.add(looking)
    }

    fun arrangeScreenPingWidget(partialTicks: Float) {
        val initial = AlignedScreenPosition.of(10, 10)
        var pos = initial.toNotAligned()
        var size = Size.ZERO
        screenPingHolograms.sortBy {
            it.getSourceScreenPosition(partialTicks).y
        }
        screenPingHolograms.forEach { state ->
            if (!state.displayed) return@forEach
            val locate = state.locate as LocateType.Screen? ?: return@forEach
            locate.setPosition(pos.x, pos.y + size.height + 5)
            pos = state.centerScreenPos
            size = state.displaySize
        }
    }

    fun renderPingScreenPrompt(style: HologramStyle, partialTicks: Float) {

        RenderSystem.setShader(CoreShaders.POSITION_COLOR)
        RenderSystem.disableCull()

        this.screenPingHolograms.forEach { state ->
            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)

            if (!state.displayed) return@forEach
            if (state.inViewDegree) {
                val locate = state.locate as LocateType.Screen? ?: return@forEach

                val widgetX = (locate.position.x + state.displaySize.width).toDouble()
                val widgetY = (locate.position.y + state.displaySize.height / 2).toDouble()
                val (worldX, worldY) = state.getSourceScreenPosition(partialTicks)

                LinkLineRender.fillThreeSegmentConnectionLine(
                    Vector2d(widgetX, widgetY),
                    Vector2d(worldX.toDouble(), worldY.toDouble()),
                    10.0,
                    20.0,
                    builder,
                    style.poseMatrix()
                )
            }
            val meshData = builder.build() ?: return
            BufferUploader.drawWithShader(meshData)
        }
    }

    fun tryPingLookingVector() {
        val looking = getLookingHologram() ?: return
        val vector = Minecraft.getInstance().gameRenderer.mainCamera.lookVector
        looking.locate = LocateType.World.FacingVector(vector.negate(Vector3f()))
    }


}
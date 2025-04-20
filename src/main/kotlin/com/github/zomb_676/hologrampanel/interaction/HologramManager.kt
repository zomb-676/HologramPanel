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
import com.github.zomb_676.hologrampanel.render.TransitRenderTargetManager
import com.github.zomb_676.hologrampanel.util.*
import com.github.zomb_676.hologrampanel.util.MousePositionManager.component1
import com.github.zomb_676.hologrampanel.util.MousePositionManager.component2
import com.github.zomb_676.hologrampanel.util.packed.AlignedScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.CoreShaders
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
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
    internal fun renderOverlayPart(guiGraphics: GuiGraphics, partialTicks: Float) = profilerStack("hologram_panel_render") {
        val context = RayTraceHelper.findTarget(32.0, partialTicks)
        if (context != null && !widgets.containsKey(context.getIdentityObject())) {
            val widget = RayTraceHelper.createHologramWidget(context, DisplayType.NORMAL)
            if (widget != null) {
                this.tryAddWidget(widget, context, DisplayType.NORMAL, listOf(HologramTicket.ByTickAfterNotSee(80)))
            }
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
            val locate = state.locate

            if (locate is LocateType.World.FacingVector) {
                state.setDisplayScale(scaleValue)
                style.stack {
                    style.scale(scaleValue)
                    state.updateDisplaySize(style.poseMatrix())
                    TransitRenderTargetManager.allocate(state.displaySize, locate, state)
                }
                state.displayed = true
                return@forEach
            }

            style.stack {
                //calculate screen position by world position
                val screenPos = state.updateRenderScreenPosition(partialTicks).equivalentSmooth(style)
                style.move(screenPos.x, screenPos.y)
                //change scale according to distance
                val scale: Double = scaleValue * if (locate is LocateType.World.FacingPlayer) {
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
                if (locate is LocateType.World) {
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
        style.guiGraphics.flush()

        this.renderFacingVectors(style, partialTicks)
        this.arrangeScreenPingWidget(partialTicks)
        this.updateLookingAt()
        this.renderFacingVectorForLooking(style, partialTicks)
        if (Config.Client.renderDebugTransientTarget.get()) {
            TransitRenderTargetManager.blitAllTransientTargetToMain(style)
        }
        this.renderPingScreenPrompt(style, partialTicks)

        if (Config.Style.renderLookIndicator.get()) {
            val distance = Config.Style.lookIndicatorDistance.get()
            val percent = Config.Style.lookIndicatorPercent.get()
            val target = getLookingHologram()
            if (target?.locate !is LocateType.World.FacingVector) {
                this.renderOverlayHologramStateTip(style, target, 0xff_00a2e8.toInt(), distance, percent)
            }
        }

        HologramInteractionManager.renderTick()

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
    private fun renderOverlayHologramStateTip(
        style: HologramStyle,
        target: HologramRenderState?,
        color: Int,
        baseOffset: Int,
        percent: Double
    ) {
        val target = target ?: return
        if (!target.displayed) return
        style.stack {
            val screenPos = target.screenPos

            val displayWidth = target.displaySize.width
            val displayHeight = target.displaySize.height

            val scaledOffset = baseOffset * target.displayScale
            val left: Double
            val right: Double
            val up: Double
            val down: Double
            when (target.locate) {
                is LocateType.Screen -> {
                    left = screenPos.x - scaledOffset
                    right = screenPos.x + displayWidth + scaledOffset
                    up = screenPos.y - scaledOffset
                    down = screenPos.y + displayHeight + scaledOffset
                }

                LocateType.World.FacingPlayer -> {
                    left = (screenPos.x - displayWidth / 2.0) - scaledOffset
                    right = (screenPos.x + displayWidth / 2.0) + scaledOffset
                    up = (screenPos.y - displayHeight / 2.0) - scaledOffset
                    down = (screenPos.y + displayHeight / 2.0) + scaledOffset
                }

                is LocateType.World.FacingVector -> {
                    val window = Minecraft.getInstance().window
                    val centerX = window.guiScaledWidth / 2
                    val centerY = window.guiScaledHeight / 2
                    left = centerX - displayWidth / 2.0 - scaledOffset
                    right = centerX + displayWidth / 2.0 + scaledOffset
                    up = centerY - displayHeight / 2.0 - scaledOffset
                    down = centerY + displayHeight / 2.0 + scaledOffset
                }
            }

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
        val (checkX, checkY) = MousePositionManager
        this.lookingWidget = this.states.values
            .asSequence()
            .filter { it.displayed }
            .firstOrNull { state ->
                when (val locate = state.locate) {
                    LocateType.World.FacingPlayer -> {
                        val size = state.displaySize
                        val position = state.screenPos
                        val left = position.x - size.width / 2
                        if (left > checkX) return@firstOrNull false
                        val right = position.x + size.width / 2
                        if (right < checkX) return@firstOrNull false
                        val up = position.y - size.height / 2
                        if (up > checkY) return@firstOrNull false
                        val down = position.y + size.height / 2
                        return@firstOrNull down > checkY
                    }

                    is LocateType.Screen -> {
                        val size = state.displaySize
                        val position = state.screenPos
                        if (checkX < position.x) return@firstOrNull false
                        if (checkY < position.y) return@firstOrNull false
                        if (checkX > position.x + size.width) return@firstOrNull false
                        if (checkY > position.y + size.height) return@firstOrNull false
                        return@firstOrNull true
                    }

                    is LocateType.World.FacingVector -> locate.isMouseIn(checkX, checkY)
                }
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

    private fun arrangeScreenPingWidget(partialTicks: Float) {
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
            pos = state.screenPos
            size = state.displaySize
        }
    }

    private fun renderPingScreenPrompt(style: HologramStyle, partialTicks: Float) {

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
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        looking.locate = LocateType.World.FacingVector().byCamera(camera)
    }

    fun renderFacingVectors(style: HologramStyle, partialTicks: Float) {
        glDebugStack("facingVectors") {
            for ((target, states) in TransitRenderTargetManager.getEntries()) {
                if (states.isEmpty()) continue
                target.bindWrite(true)
                OpenGLStateManager.preventMainBindWrite {
                    for (state in states) {
                        val locate = state.locate as? LocateType.World.FacingVector? ?: continue
                        val rect = locate.allocatedSpace
                        style.stack {
                            style.move(rect.x, rect.y)
                            style.scale(state.displayScale)
                            style.drawFullyBackground(state.size)
                            state.widget.render(state, style, state.displayType, partialTicks)
                        }
                    }
                    style.guiGraphics.flush()
                }
            }
            Minecraft.getInstance().mainRenderTarget.bindWrite(true)
        }
    }

    fun renderFacingVectorForLooking(style: HologramStyle, partialTick: Float) = glDebugStack("facingVectorForLooking") {
        val target = this.getLookingHologram() ?: return@glDebugStack
        if (!target.displayed) return@glDebugStack
        val locate = target.locate as? LocateType.World.FacingVector? ?: return@glDebugStack
        val renderTarget = TransitRenderTargetManager.getLookingTarget()

        val window = Minecraft.getInstance().window
        OpenGLStateManager.preventMainBindWrite {
            MousePositionManager.remappingMouseForLooking(target) {
                val (u, v) = MousePositionManager
                style.stack {
                    renderTarget.bindWrite(true)
                    style.translate(window.guiScaledWidth / 2f, window.guiScaledHeight / 2f)
                    style.scale(target.displayScale)
                    run {
                        val size = target.size
                        style.translate(-size.width / 2f, -size.height / 2f)
                        style.drawFullyBackground(target.size)
                        MousePositionManager.relocateOriginPoint(style.poseMatrix()) {
                            style.stack {
                                target.widget.render(target, style, target.displayType, partialTick)
                            }
                            this.getInteractiveTarget()?.renderInteractive(style, target.size, partialTick)
                        }
                    }
                    if (Config.Client.renderLookingTransientReMappingIndicator.get()) {
                        style.stack {
                            style.scale(1 / target.displayScale)
                            style.drawVerticalLine(-10000, +10000, u.toInt(), -1)
                            style.drawHorizontalLine(-10000, +10000, v.toInt(), -1)
                        }
                        style.drawString("u:%.2f,v:%.2f".format(u, v), 0, -30)
                    }
                }
                style.guiGraphics.flush()
            }
            glDebugStack("indicator") {
                if (Config.Style.renderLookIndicator.get()) {
                    val distance = Config.Style.lookIndicatorDistance.get()
                    val percent = Config.Style.lookIndicatorPercent.get()
                    this.renderOverlayHologramStateTip(
                        style, target, 0xff_00a2e8.toInt(), distance, percent
                    )
                    style.guiGraphics.flush()
                }
            }
        }
        Minecraft.getInstance().mainRenderTarget.bindWrite(true)
    }

    fun renderWorldPart(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return
        val pose = event.poseStack
        val partialTick = event.partialTick.getGameTimeDeltaPartialTick(false)
        val camPos = event.camera.position
        pose.translate(-camPos.x, -camPos.y, -camPos.z)

        RenderSystem.enableBlend()
        RenderSystem.disableCull()
        RenderSystem.setShader(CoreShaders.POSITION_TEX)
        glDebugStack("world") {
            for ((target, states) in TransitRenderTargetManager.getEntries()) {
                if (states.isEmpty()) continue
                RenderSystem.setShaderTexture(0, target.colorTextureId)
                val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
                for (state in states) {
                    val locate = state.locate as? LocateType.World.FacingVector? ?: continue
                    val center = state.sourcePosition(partialTick)
                    val (width, height) = state.displaySize
                    //TODO the 80 here should be replaced by value calculated by the projection matrix
                    val left = locate.getLeft().mul(width / 2f / 80f, Vector3f())
                    val up = locate.getUp().mul(height / 2f / 80f, Vector3f())

                    fun Vector3f.add(): VertexConsumer {
                        return builder.addVertex(pose.last().pose(), this.x, this.y, this.z)
                    }

                    val window = Minecraft.getInstance().window
                    val rect = locate.allocatedSpace
                    //one minus is because allocator's up-down is different from uv
                    val u0 = (rect.x / window.guiScaledWidth.toFloat())
                    val v1 = 1 - (rect.y / window.guiScaledHeight.toFloat())
                    val u1 = ((rect.x + rect.width) / window.guiScaledWidth.toFloat())
                    val v0 = 1 - ((rect.y + rect.height) / window.guiScaledHeight.toFloat())

                    val containerVector = Vector3f()
                    //left-up
                    containerVector.set(center).add(left).add(up).apply {
                        locate.updateLeftUp(this)
                        if (!state.isLookingAt()) add().setUv(u0, v1)
                    }
                    //left-down
                    containerVector.set(center).add(left).sub(up).apply {
                        locate.updateLeftDown(this)
                        if (!state.isLookingAt()) add().setUv(u0, v0)
                    }
                    //right-down
                    containerVector.set(center).sub(left).sub(up).apply {
                        locate.updateRightDown(this)
                        if (!state.isLookingAt()) add().setUv(u1, v0)
                    }
                    //right-up
                    containerVector.set(center).sub(left).add(up).apply {
                        locate.updateRightUp(this)
                        if (!state.isLookingAt()) add().setUv(u1, v1)
                    }
                }
                builder.build()?.apply(BufferUploader::drawWithShader)
            }
            getLookingHologram()?.also { state ->
                val locate = state.locate as? LocateType.World.FacingVector? ?: return@also
                val center = state.sourcePosition(partialTick)
                val window = Minecraft.getInstance().window
                val width = window.guiScaledWidth
                val height = window.guiScaledHeight

                val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
                RenderSystem.setShaderTexture(0, TransitRenderTargetManager.getLookingTarget().colorTextureId)

                //TODO the 80 here should be replaced by value calculated by the projection matrix
                val left = locate.getLeft().mul(width / 2f / 80f, Vector3f())
                val up = locate.getUp().mul(height / 2f / 80f, Vector3f())

                fun Vector3f.add(): VertexConsumer {
                    return builder.addVertex(pose.last().pose(), this.x, this.y, this.z)
                }

                val u0 = 0.0f
                val v1 = 1.0f
                val u1 = 1.0f
                val v0 = 0.0f

                val containerVector = Vector3f()
                //left-up
                containerVector.set(center).add(left).add(up).add().setUv(u0, v1)
                //left-down
                containerVector.set(center).add(left).sub(up).add().setUv(u0, v0)
                //right-down
                containerVector.set(center).sub(left).sub(up).add().setUv(u1, v0)
                //right-up
                containerVector.set(center).sub(left).add(up).add().setUv(u1, v1)

                BufferUploader.drawWithShader(builder.buildOrThrow())
            }
            TransitRenderTargetManager.refresh()
        }
        glDebugStack("clear") {
            TransitRenderTargetManager.getEntries().forEach { (target, _) ->
                target.clear()
            }
            TransitRenderTargetManager.getLookingTarget().clear()
        }
        Minecraft.getInstance().mainRenderTarget.bindWrite(true)
    }
}
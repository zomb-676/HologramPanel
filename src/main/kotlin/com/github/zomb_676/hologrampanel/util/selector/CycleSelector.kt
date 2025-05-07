package com.github.zomb_676.hologrampanel.util.selector

import com.github.zomb_676.hologrampanel.PanelOperatorManager
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.MouseInputModeUtil
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector.Companion.tryBegin
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector.Companion.tryEnd
import com.github.zomb_676.hologrampanel.util.stack
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * cycle selector for some convenience operations in game
 *
 * call [tryBegin] to trig a CycleSelector and [tryEnd] to close
 *
 * use [CycleSelectorBuilder] to builder the actual instance
 */
class CycleSelector(topEntry: CycleEntry.Group) : CycleEntry.SelectorCallback {

    /**
     * used to trace open trace and return to parent
     */
    private val openStack = mutableListOf<CycleEntry.Group>()

    /**
     * the current group that is interacted
     */
    private var currentGroup = topEntry

    /**
     * the next group that will be used if not null
     */
    private var nextGroup: CycleEntry.Group? = null

    /**
     * the current entry which mouse is over
     */
    private var current: CycleEntry? = null

    private var canBackToParent = false

    private fun render(graphics: GuiGraphics, tracker: DeltaTracker) {
        this.current = null
        run {
            val nextChildren = this.nextGroup
            if (nextChildren != null) {
                this.currentGroup = nextChildren
                this.nextGroup = null
            }
        }

        val partialTick = tracker.getGameTimeDeltaPartialTick(false)
        val style = HologramStyle.DefaultStyle(graphics)
        val window = Minecraft.getInstance().window
        val centerX = window.guiScaledWidth / 2
        val centerY = window.guiScaledHeight / 2
        style.guiGraphics.pose().pushPose()
        style.move(centerX, centerY)

        RenderSystem.enableBlend()

        val scale = window.guiScale
        val handler = Minecraft.getInstance().mouseHandler
        val x = ((handler.xpos() / scale) - centerX)
        val y = ((handler.ypos() / scale) - centerY)
        val degree = run {
            val degree = Math.toDegrees(atan(x / y))
            if (y < 0) {
                180 + degree
            } else {
                if (x < 0) {
                    degree + 360
                } else {
                    degree
                }
            }
        }

        val sqrt = sqrt(x * x + y * y)
        val canSelect = sqrt > 20

        val currentVisible = currentGroup.children()
            .onEach(CycleEntry::tick)
            .filter(CycleEntry::isVisible)

        val currentRadian = Math.toRadians(degree)
        val degreeForEach = 360.0 / currentVisible.size
        currentVisible.forEachIndexed { index, entry ->
            val from = Math.toRadians((degreeForEach * index) + 1)
            val to = Math.toRadians((degreeForEach * (index + 1)) - 1)
            val isHover = canSelect && currentRadian in from..to
            val color = if (isHover) {
                this.current = entry
                0xffffff00.toInt()
            } else {
                0x7fffffff.toInt()
            }
            RenderSystem.enableBlend()
            style.drawTorus(
                20f, 70f, colorOut = color, colorIn = 0x7fffffff,
                beginRadian = from, endRadian = to, isClockWise = false
            )
            RenderSystem.disableBlend()

            val distance = 45f
            val centerDegree = (from + to) / 2
            val size = entry.size(style)
            style.stack {
                style.translate(
                    (sin(centerDegree) * distance),
                    (cos(centerDegree) * distance)
                )
                style.scale(entry.scale())
                style.translate(-size.width / 2f, -size.height / 2f)
                entry.renderContent(style, partialTick, isHover)
            }
        }

        this.canBackToParent = !canSelect && sqrt < 20 * 0.8 && this.openStack.isNotEmpty()
        RenderSystem.enableBlend()
        if (this.canBackToParent) {
            style.drawCycle(16f, colorOut = 0xffffff00.toInt(), colorIn = 0x7fffffff.toInt())
        } else if (this.openStack.isNotEmpty()) {
            style.drawCycle(16f, colorOut = 0x7fffffff.toInt())
        }
        RenderSystem.disableBlend()

        style.guiGraphics.flush()
        RenderSystem.disableBlend()
        style.guiGraphics.pose().popPose()
    }

    override fun openGroup(group: CycleEntry.Group) {
        this.nextGroup = group
        this.openStack.addLast(this.currentGroup)
    }

    override fun recoveryToParent() {
        val next = this.openStack.removeLastOrNull()
        if (next != null) {
            this.nextGroup = next
        }
    }

    companion object {

        fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
            val instance = instance ?: return
            instance.render(guiGraphics, deltaTracker)
        }


        fun tryBegin() {
            if (this.instance == null) {
                MouseInputModeUtil.tryEnter()
//                this.instance = CycleSelectorBuilder {
//                    repeat(5) { index ->
//                        add(ComponentRenderElement(index.toString())) {
//                            Minecraft.getInstance().gui.chat.addMessage(Component.literal("$index"))
//                        }
//                    }
//
//                }
                this.instance = PanelOperatorManager.createInstance()
            }
        }

        fun tryEnd() {
            val selector = this.instance ?: return
            instance?.current?.onClose(selector)
            this.instance = null
            MouseInputModeUtil.exit()
        }

        fun onClick() {
            val selector = this.instance ?: return
            if (selector.canBackToParent) {
                selector.recoveryToParent()
            } else {
                val target = selector.current ?: return
                target.onClick(selector, CycleEntry.TrigType.BY_CLICK)
            }
        }

        private var instance: CycleSelector? = null

        fun instanceExist() = instance != null
    }
}
package com.github.zomb_676.hologrampanel.util.selector

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.dynamic.IRenderElement
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
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
        trySetModeAndRestCursorPos()

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

        val currentRadian = Math.toRadians(degree)
        val degreeForEach = 360.0 / currentGroup.childrenCount()
        currentGroup.children().forEachIndexed { index, entry ->
            val from = Math.toRadians((degreeForEach * index) + 1)
            val to = Math.toRadians((degreeForEach * (index + 1)) - 1)
            val isHover = canSelect && currentRadian in from..to
            val color = if (isHover) {
                this.current = entry
                0xffffff00.toInt()
            } else {
                0x7fffffff.toInt()
            }
            style.drawTorus(
                20f, 70f, colorOut = color, colorIn = 0x7fffffff,
                beginRadian = from, endRadian = to, isClockWise = false
            )

            val distance = 45f
            val centerDegree = (from + to) / 2
            val size = entry.size(style)
            style.stack {
                style.translate(
                    (sin(centerDegree) * distance) - (size.width / 2),
                    (cos(centerDegree) * distance) - (size.height / 2)
                )
                entry.renderContent(style, partialTick, isHover)
            }
        }

        this.canBackToParent = !canSelect && sqrt < 20 * 0.8 && this.openStack.isNotEmpty()
        if (this.canBackToParent) {
            style.drawCycle(16f, colorOut = 0xffffff00.toInt(), colorIn = 0x7fffffff.toInt())
        } else if (this.openStack.isNotEmpty()) {
            style.drawCycle(16f, colorOut = 0x7fffffff.toInt())
        }

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
        private fun trySetModeAndRestCursorPos() {
            val window = Minecraft.getInstance().window
            if (GLFW.glfwGetInputMode(window.window, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_NORMAL) {
                setCursorMode(GLFW.GLFW_CURSOR_NORMAL)
                GLFW.glfwSetCursorPos(window.window, (window.width / 2).toDouble(), (window.height / 2).toDouble())
            }
        }

        private fun setCursorMode(cursorMode: Int) {
            GLFW.glfwSetInputMode(Minecraft.getInstance().window.window, GLFW.GLFW_CURSOR, cursorMode)
        }

        fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
            val instance = instance ?: return
            instance.render(guiGraphics, deltaTracker)
        }

        @JvmStatic
        fun preventPlayerTurn() = this.instance != null
        fun tryBegin() {
            if (this.instance == null) {
                val builder = CycleSelectorBuilder()
                this.instance = builder.buildScope {
                    repeat(5) { index ->
                        add(IRenderElement.StringRenderElement(index.toString())) {
                            Minecraft.getInstance().gui.chat.addMessage(Component.literal("$index"))
                        }
                    }
                    addGroup(IRenderElement.ItemStackElement(false, ItemStack(Items.DIRT))) {
                        repeat(5) { index ->
                            add(IRenderElement.StringRenderElement("inner$index")) {
                                Minecraft.getInstance().gui.chat.addMessage(Component.literal("inner$index"))
                            }
                        }
                    }
                }
            }
        }

        fun tryEnd() {
            if (this.instance != null) {
                instance?.current?.onClose()

                this.instance = null
                if (Minecraft.getInstance().screen == null) {
                    setCursorMode(GLFW.GLFW_CURSOR_DISABLED)
                } else {
                    setCursorMode(GLFW.GLFW_CURSOR_NORMAL)
                }
            }
        }

        fun onClick() {
            val selector = this.instance ?: return
            if (selector.canBackToParent) {
                selector.recoveryToParent()
            } else {
                val target = selector.current ?: return
                target.onClick(selector)
            }
        }

        private var instance: CycleSelector? = null
    }
}
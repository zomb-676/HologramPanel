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

class CycleSelector(topEntry: CycleEntry.Group) : CycleEntry.SelectorCallback {

    private val openStack = mutableListOf<CycleEntry.Group>()
    private var currentGroup = topEntry
    private var nextGroup: CycleEntry.Group? = null
    private var current: CycleEntry? = null

    fun render(graphics: GuiGraphics, tracker: DeltaTracker) {
        trySetModeAndRestCursorPos()

        this.current = null
        run {
            val nextChildren = this.nextGroup
            if (nextChildren != null) {
                this.openStack.addLast(this.currentGroup)
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
        val d = Math.toDegrees(atan(x / y))
        val degree = if (y < 0) {
            180 + d
        } else {
            if (x < 0) {
                d + 360
            } else {
                d
            }
        }

        val canSelect = sqrt(x * x + y * y) > 20

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
        style.guiGraphics.flush()
        RenderSystem.disableBlend()
        style.guiGraphics.pose().popPose()
    }

    override fun openGroup(group: CycleEntry.Group) {
        this.nextGroup = group
    }

    override fun recoveryToParent(child: CycleEntry) {
        val next = this.openStack.removeLastOrNull()
        if (next != null) {
            this.nextGroup = next
        }
    }

    companion object {
        fun trySetModeAndRestCursorPos() {
            val window = Minecraft.getInstance().window
            if (GLFW.glfwGetInputMode(window.window, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_NORMAL) {
                setCursorMode(GLFW.GLFW_CURSOR_NORMAL)
                GLFW.glfwSetCursorPos(window.window, (window.width / 2).toDouble(), (window.height / 2).toDouble())
            }
        }

        fun setCursorMode(cursorMode: Int) {
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
            val target = selector.current ?: return
            target.onClick(selector)
        }

        var instance: CycleSelector? = null
    }
}
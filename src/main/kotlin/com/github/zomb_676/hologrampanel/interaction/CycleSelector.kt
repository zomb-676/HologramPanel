package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.util.stack
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.CoreShaders
import net.minecraft.network.chat.Component
import kotlin.math.cos
import kotlin.math.sin

class CycleSelectorScreen(entries: List<CycleEntry>, val outRadius: Float, val innerRadius: Float = 0f) : Screen(
    Component.literal("cycle_selector_screen")
) {

    var beginDegree = 0.0

    private val entries = entries.mapIndexed { index, entry ->
        CycleEntryData(entry, index, entries.size)
    }

    fun render(graphics: GuiGraphics, tracker: DeltaTracker) {
        return
        val partialTick = tracker.getGameTimeDeltaPartialTick(false)

        this.renderCyclesAndSplitLine(graphics, partialTick)

        entries.forEach { entry ->
            entry.entry.render(graphics, entry, partialTick)
        }
    }

    private fun renderCyclesAndSplitLine(graphics: GuiGraphics, partialTick: Float) {
        entries.size
        val window = Minecraft.getInstance().window
        graphics.stack {
            graphics.pose().translate(window.guiScaledWidth / 2.0f, window.guiScaledHeight / 2.0f, 0.0f)

            val color = 0x7fffffff.toInt()

            RenderSystem.setShader(CoreShaders.POSITION_COLOR)
            RenderSystem.enableBlend()
            run {
                val tesselator = Tesselator.getInstance()
                val builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR)


                val matrix = graphics.pose().last().pose()
                builder.addVertex(matrix, 0f, 0f, 0f).setColor(0x26ffffff)

                var radius = 0.0
                val step = Math.toRadians(360.0 / TESSELLATION_COUNT).toFloat()
                builder.addVertex(matrix, 0.0f, outRadius, 0.0f).setColor(color)
                while (true) {
                    radius += step
                    if (radius < Math.PI * 2) {
                        builder.addVertex(
                            matrix,
                            sin(radius).toFloat() * outRadius,
                            cos(radius).toFloat() * outRadius,
                            0.0f
                        ).setColor(color)
                    } else {
                        break
                    }
                }
                builder.addVertex(matrix, 0.0f, outRadius, 0.0f).setColor(color)

                BufferUploader.drawWithShader(builder.buildOrThrow())
                RenderSystem.disableBlend()
            }
            run {
                //todo draw split line
            }
        }
    }


    companion object {
        const val TESSELLATION_COUNT = 180
    }

    class CycleEntryData(val entry: CycleEntry, val index: Int, val total: Int)

    class CycleEntry() {
        fun render(graphics: GuiGraphics, data: CycleEntryData, partialTick: Float) {

        }
    }
}
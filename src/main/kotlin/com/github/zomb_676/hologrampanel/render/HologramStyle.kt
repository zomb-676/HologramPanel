package com.github.zomb_676.hologrampanel.render

import com.github.zomb_676.hologrampanel.util.ScreenPosition
import com.github.zomb_676.hologrampanel.util.SelectPathType
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.normalizedInto2PI
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.CoreShaders
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.world.item.DyeColor
import kotlin.math.*

interface HologramStyle {
    val guiGraphics: GuiGraphics
    var contextColor: Int


    fun mergeOutlineSizeForSingle(contentSize: Size): Size
    fun mergeOutlineSizeForGroup(contentSize: Size, descriptionSize: Size, collapse: Boolean): Size
    fun drawSingleOutline(size: Size, selected: SelectPathType, color: Int = contextColor)
    fun drawGroupOutline(size: Size, selected: SelectPathType, collapse: Boolean, color: Int = contextColor)
    fun moveToGroupDescription()
    fun moveAfterDrawGroupOutline(descriptionSize: Size)
    fun moveAfterDrawSingleOutline()
    fun mergeOutlineSizeForSlot(contentSize: Size): Size
    fun drawSlotOutline(sizeIncludingOutline: Size)
    fun moveAfterDrawSlotOutline()


    fun drawString(string: String, x: Int = 0, y: Int = 0, color: Int = DyeColor.BLACK.textColor) {
        guiGraphics.drawString(font, string, x, y, color, false)
    }

    fun drawString(string: Component, x: Int = 0, y: Int = 0, color: Int = DyeColor.BLACK.textColor) {
        guiGraphics.drawString(font, string, x, y, color, false)
    }

    fun drawHorizontalLine(left: Int, right: Int, y: Int, color: Int = contextColor) {
        guiGraphics.hLine(left, right, y, color)
    }

    fun drawVerticalLine(up: Int, down: Int, x: Int, color: Int = contextColor) {
        guiGraphics.vLine(x, up, down, color)
    }

    fun move(x: Int, y: Int) {
        this.translate(x.toFloat(), y.toFloat())
    }

    fun move(screenPosition: ScreenPosition) {
        this.move(screenPosition.x, screenPosition.y)
    }

    fun translate(x: Float, y: Float) {
        this.translate(x.toDouble(), y.toDouble())
    }

    fun translate(x: Double, y: Double) {
        guiGraphics.pose().translate(x, y, 1.0)
    }

    fun fill(size: Size, color: Int = contextColor) {
        fill(0, 0, size.width, size.height, contextColor)
    }

    fun fill(minX: Int, minY: Int, maxX: Int, maxY: Int, color: Int = contextColor) {
        guiGraphics.fill(minX, minY, maxX, maxY, color)
    }

    fun fill(minX: Double, minY: Double, maxX: Double, maxY: Double, color: Int = contextColor) {
        fill(minX.toFloat(), minY.toFloat(), maxX.toFloat(), maxY.toFloat())
    }

    fun fill(minX: Float, minY: Float, maxX: Float, maxY: Float, color: Int = contextColor) {
        val consumer = guiGraphics.bufferSource.getBuffer(RenderType.gui())
        val pose = guiGraphics.pose().last().pose()
        consumer.addVertex(pose, minX, minY, 0.0f).setColor(color)
        consumer.addVertex(pose, minX, maxY, 0.0f).setColor(color)
        consumer.addVertex(pose, maxX, maxY, 0.0f).setColor(color)
        consumer.addVertex(pose, maxX, minY, 0.0f).setColor(color)
    }

    fun scale(x: Double, y: Double) {
        this.scale(x.toFloat(), y.toFloat())
    }

    fun scale(x: Float, y: Float) {
        guiGraphics.pose().scale(x, y, 1.0f)
    }

    fun scale(scale: Double) {
        this.scale(scale, scale)
    }

    fun scale(scale: Float) {
        this.scale(scale, scale)
    }

    val font: Font get() = Minecraft.getInstance().font

    fun measureString(string: String): Size {
        return Size.of(font.width(string), font.lineHeight)
    }

    fun measureString(string: Component): Size {
        return Size.of(font.width(string), font.lineHeight)
    }

    fun itemStackSize(): Size = ITEM_STACK_SIZE

    fun outline(size: Size, color: Int = contextColor) {
        guiGraphics.renderOutline(0, 0, size.width, size.height, color)
    }

    fun drawCycle(
        outRadius: Float,
        colorOut: Int = contextColor,
        colorIn: Int = colorOut,
        beginRadian: Double = Math.PI * 2,
        endRadian: Double = 0.0,
        tessellationCount: Int = 180,
        isClockWise: Boolean = true
    ) {
        require(outRadius > 0)
        var endRadian = endRadian.normalizedInto2PI
        var beginRadian = beginRadian.normalizedInto2PI
        if (isClockWise) {
            if (endRadian > beginRadian) {
                endRadian -= Math.PI * 2
            }
            val temp = beginRadian
            beginRadian = endRadian
            endRadian = temp
        } else {
            if (endRadian < beginRadian) {
                endRadian += Math.PI * 2
            }
        }

        RenderSystem.setShader(CoreShaders.POSITION_COLOR)
        val tesselator = Tesselator.getInstance()
        val builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR)

        val matrix = guiGraphics.pose().last().pose()
        builder.addVertex(matrix, 0f, 0f, 0f).setColor(colorIn)

        var radius = beginRadian
        val step = Math.toRadians(360.0 / tessellationCount).toFloat()
        while (true) {
            if (abs(radius - endRadian) > abs(step)) {
                builder.addVertex(
                    matrix,
                    sin(radius).toFloat() * outRadius,
                    cos(radius).toFloat() * outRadius,
                    0.0f
                ).setColor(colorOut)
                radius += step
            } else {
                break
            }
        }
        builder.addVertex(matrix, sin(endRadian).toFloat() * outRadius, cos(endRadian).toFloat() * outRadius, 0.0f)
            .setColor(colorOut)

        BufferUploader.drawWithShader(builder.buildOrThrow())
    }

    fun drawTorus(
        inRadius: Float,
        outRadius: Float,
        colorOut: Int = contextColor,
        colorIn: Int = colorOut,
        beginRadian: Double = Math.PI * 2,
        endRadian: Double = 0.0,
        tessellationCount: Int = 180,
        isClockWise: Boolean = true
    ) {
        require(outRadius > inRadius)
        if (inRadius == 0.0f) {
            drawCycle(outRadius, colorOut, colorIn, beginRadian, endRadian, tessellationCount, isClockWise)
            return
        }
        require(inRadius > 0)

        var endRadian = endRadian.normalizedInto2PI
        var beginRadian = beginRadian.normalizedInto2PI
        if (isClockWise) {
            if (endRadian > beginRadian) {
                endRadian -= Math.PI * 2
            }
            val temp = beginRadian
            beginRadian = endRadian
            endRadian = temp
        } else {
            if (endRadian < beginRadian) {
                endRadian += Math.PI * 2
            }
        }

        RenderSystem.setShader(CoreShaders.POSITION_COLOR)
        val tesselator = Tesselator.getInstance()
        val builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)

        val matrix = guiGraphics.pose().last().pose()

        var radius = beginRadian
        val step = Math.toRadians(360.0 / tessellationCount).toFloat()

        fun fillVertex(fillVertexRadius: Double) {
            val lastSin = sin(fillVertexRadius).toFloat()
            val lastCos = cos(fillVertexRadius).toFloat()
            builder.addVertex(matrix, lastSin * inRadius, lastCos * inRadius, 0.0f).setColor(colorIn)
            builder.addVertex(matrix, lastSin * outRadius, lastCos * outRadius, 0.0f).setColor(colorOut)
        }

        while (true) {
            if (abs(radius - endRadian) > abs(step)) {
                fillVertex(radius)
                radius += step
            } else {
                fillVertex(endRadian)
                break
            }
        }

        BufferUploader.drawWithShader(builder.buildOrThrow())
    }

    companion object {
        const val ITEM_STACK_LENGTH = 16
        val ITEM_STACK_SIZE = Size.of(ITEM_STACK_LENGTH, ITEM_STACK_LENGTH)
    }

    class DefaultStyle(override val guiGraphics: GuiGraphics) : HologramStyle {
        override var contextColor: Int = (0xff000000).toInt()

        override fun mergeOutlineSizeForSingle(contentSize: Size): Size {
            return contentSize.expandWidth(4).expandHeight(5)
        }

        override fun mergeOutlineSizeForGroup(contentSize: Size, descriptionSize: Size, collapse: Boolean): Size {
            if (collapse) {
                val width = max(contentSize.width + 6, descriptionSize.width + 8)
                val height = contentSize.height + descriptionSize.height
                return Size.of(width + 4, height + 4)
            } else {
                val width = max(contentSize.width + 6, descriptionSize.width + 8)
                val height = contentSize.height + descriptionSize.height
                return Size.of(width + 4, height + 6)
            }
        }

        override fun drawSingleOutline(size: Size, selected: SelectPathType, color: Int) {
            if (selected.isAtTerminal) {
                outline(size.shrinkHeight(2), brightColorBySelectedType(color, selected))
            }
        }

        fun brightColorBySelectedType(color: Int, selected: SelectPathType) = when {
            selected.isUnSelect -> 0xff000000.toInt()
            selected.isAtTerminal -> 0xffffffff.toInt()
            else  -> 0xff7f7f7f.toInt()
        }

        fun brighter(color: Int): Int {
            val factor = 0.7
            var r: Int = ARGB.red(color)
            var g: Int = ARGB.green(color)
            var b: Int = ARGB.blue(color)
            val alpha: Int = ARGB.alpha(color)

            val i = (1.0 / (1.0 - factor)).toInt()
            if (r == 0 && g == 0 && b == 0) {
                return ARGB.color(alpha, i, i, i)
            }
            if (r > 0 && r < i) r = i
            if (g > 0 && g < i) g = i
            if (b > 0 && b < i) b = i

            return ARGB.color(
                alpha,
                min((r / factor).toInt().toDouble(), 255.0).toInt(),
                min((g / factor).toInt().toDouble(), 255.0).toInt(),
                min((b / factor).toInt().toDouble(), 255.0).toInt(),
            )
        }

        override fun drawGroupOutline(size: Size, selected: SelectPathType, collapse: Boolean, color: Int) {
            val height = if (selected.isAtHead) {
                size.height
            } else {
                size.height - 2
            }
            guiGraphics.renderOutline(0, 0, size.width, height, brightColorBySelectedType(color, selected))

            val matrix = guiGraphics.pose().last().pose()
            val consumer = guiGraphics.bufferSource.getBuffer(RenderType.gui())
            val left = 4.0f
            val right = 8.0f
            val up = 4.0f
            val down = 8.0f

            consumer.addVertex(matrix, left, 5.5f, 0.0f).setColor(color)
            consumer.addVertex(matrix, left, 6.5f, 0.0f).setColor(color)
            consumer.addVertex(matrix, right, 6.5f, 0.0f).setColor(color)
            consumer.addVertex(matrix, right, 5.5f, 0.0f).setColor(color)
            if (!collapse) {
                consumer.addVertex(matrix, 5.5f, up, 0.0f).setColor(color)
                consumer.addVertex(matrix, 5.5f, down, 0.0f).setColor(color)
                consumer.addVertex(matrix, 6.5f, down, 0.0f).setColor(color)
                consumer.addVertex(matrix, 6.5f, up, 0.0f).setColor(color)
            }
        }

        override fun moveToGroupDescription() {
            move(10, 2)
        }

        override fun moveAfterDrawGroupOutline(descriptionSize: Size) {
            move(6, 4 + descriptionSize.height)
        }

        override fun moveAfterDrawSingleOutline() {
            move(2, 2)
        }

        override fun mergeOutlineSizeForSlot(contentSize: Size): Size {
            return contentSize.expandWidth(4).expandHeight(4)
        }

        override fun drawSlotOutline(sizeIncludingOutline: Size) {
            guiGraphics.renderOutline(0, 0, sizeIncludingOutline.width, sizeIncludingOutline.height, contextColor)
        }

        override fun moveAfterDrawSlotOutline() {
            move(2, 2)
        }
    }
}
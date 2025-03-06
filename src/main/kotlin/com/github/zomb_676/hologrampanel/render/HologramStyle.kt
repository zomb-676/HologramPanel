package com.github.zomb_676.hologrampanel.render

import com.github.zomb_676.hologrampanel.util.SelectPathType
import com.github.zomb_676.hologrampanel.util.Size
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.world.item.DyeColor
import kotlin.math.max
import kotlin.math.min

interface HologramStyle {
    val guiGraphics: GuiGraphics
    var contextColor: Int


    fun mergeOutlineSizeForSingle(contentSize: Size): Size
    fun mergeOutlineSizeForGroup(contentSize: Size, descriptionSize: Size, collapse: Boolean): Size
    fun drawSingleOutline(size: Size, selected: SelectPathType, color: Int = contextColor)
    fun drawGroupOutline(size: Size, selected: SelectPathType, color: Int = contextColor)
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

    fun scale(x: Double, y: Double) {
        this.scale(x.toFloat(), y.toFloat())
    }

    fun scale(x: Float, y: Float) {
        guiGraphics.pose().scale(x, y, 1.0f)
    }

    val font: Font get() = Minecraft.getInstance().font

    fun measureString(string: String): Size {
        return Size.of(font.width(string), font.lineHeight)
    }

    fun measureString(string: Component): Size {
        return Size.of(font.width(string), font.lineHeight)
    }

    fun itemStackSize(): Size = ITEM_STACK_SIZE


    companion object {
        inline fun HologramStyle.poseStore(pose: PoseStack, code: () -> Unit) {
            val back = guiGraphics.pose
            guiGraphics.pose = pose
            code.invoke()
            guiGraphics.pose = back
        }

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

        }

        fun brightColorBySelectedType(color: Int, selected: SelectPathType) = when (selected) {
            SelectPathType.UN_SELECTED -> 0xff000000.toInt()
            SelectPathType.ON_NONE_TERMINAL_PATH -> 0xff7f7f7f.toInt()
            SelectPathType.ON_TERMINAL -> 0xffffffff.toInt()
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

        override fun drawGroupOutline(size: Size, selected: SelectPathType, color: Int) {
            guiGraphics.renderOutline(0, 0, size.width, size.height, brightColorBySelectedType(color, selected))

            val matrix = guiGraphics.pose().last().pose()
            val consumer = guiGraphics.bufferSource.getBuffer(RenderType.gui())
            consumer.addVertex(matrix, -1 + 4f, 2.0f, 0.0f).setColor(0xff0000ff.toInt())
            consumer.addVertex(matrix, -1 + 4f, 10f, 0.0f).setColor(0xff0000ff.toInt())
            consumer.addVertex(matrix, -1 + 10f, 6f, 0.0f).setColor(0xff0000ff.toInt())
            consumer.addVertex(matrix, -1 + 10f, 6f, 0.0f).setColor(0xff0000ff.toInt())
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
package com.github.zomb_676.hologrampanel.render

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.util.*
import com.github.zomb_676.hologrampanel.util.packed.AlignedScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.CoreShaders
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import org.jetbrains.annotations.ApiStatus
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * a wrapped [GuiGraphics] which is intended for convenience use and customize widget style
 */
interface HologramStyle {
    val guiGraphics: GuiGraphics
    var contextColor: Int

    fun drawFullyBackground(size : Size)

    /**
     * @param contentSize [HologramWidgetComponent.Single.contentSize]
     * @return [HologramWidgetComponent.Single.visualSize]
     */
    fun mergeOutlineSizeForSingle(contentSize: Size): Size

    /**
     * @param contentSize [HologramWidgetComponent.Group.contentSize]
     * @param collapse if the group is collapsed or not
     */
    fun mergeOutlineSizeForGroup(contentSize: Size, descriptionSize: Size, collapse: Boolean): Size

    /**
     * @param size [HologramWidgetComponent.Single.visualSize]
     */
    fun drawSingleOutline(size: Size, color: Int = contextColor)

    /**
     * @param size [HologramWidgetComponent.Group.visualSize]
     */
    fun drawGroupOutline(
        isGroupGlobal: Boolean, size: Size, descriptionSize: Size, collapse: Boolean, color: Int = contextColor
    )

    fun moveToGroupDescription(descriptionSize: Size)
    fun moveAfterDrawGroupOutline(descriptionSize: Size)
    fun moveAfterDrawSingleOutline()

    fun drawOutlineSelected(size: Size)

    @EfficientConst
    fun elementPadding(): Int

    fun drawString(string: String, x: Int = 0, y: Int = 0, color: Int = DyeColor.BLACK.textColor) {
        guiGraphics.drawString(font, string, x, y, color, false)
    }

    fun drawString(string: Component, x: Int = 0, y: Int = 0, color: Int = DyeColor.BLACK.textColor) {
        guiGraphics.drawString(font, string, x, y, color, false)
    }

    fun drawString(string: FormattedCharSequence, x: Int = 0, y: Int = 0, color: Int = DyeColor.BLACK.textColor) {
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

    fun move(alignedScreenPosition: AlignedScreenPosition) {
        this.move(alignedScreenPosition.x, alignedScreenPosition.y)
    }

    fun translate(x: Float, y: Float, z: Float = 0.0f) {
        this.translate(x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun translate(x: Double, y: Double, z: Double = 0.0) {
        pose().translate(x, y, z)
    }

    fun fill(size: Size, color: Int = contextColor) {
        fill(0, 0, size.width, size.height, color)
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

    /**
     * @param z by default, is 1.0
     */
    fun scale(x: Double, y: Double, z: Double = 1.0) {
        this.scale(x.toFloat(), y.toFloat(), z.toFloat())
    }

    /**
     * @param z by default, is 1.0
     */
    fun scale(x: Float, y: Float, z: Float = 1.0f) {
        guiGraphics.pose().scale(x, y, z)
    }

    /**
     * not scale z
     */
    fun scale(scale: Double) {
        this.scale(scale, scale, 1.0)
    }

    /**
     * not scale z
     */
    fun scale(scale: Float) {
        this.scale(scale, scale, 1.0f)
    }

    fun mulPose(matrix: Matrix4f) {
        this.pose().mulPose(matrix)
    }

    fun mulPose(quaternion: Quaternionf) {
        this.pose().mulPose(quaternion)
    }

    fun push() = this.guiGraphics.pose().pushPose()
    fun pop() = this.guiGraphics.pose().popPose()

    fun pose(): PoseStack = this.guiGraphics.pose()
    fun poseMatrix(): Matrix4f = this.guiGraphics.pose().last().pose()

    val font: Font get() = Minecraft.getInstance().font

    fun measureString(string: String): Size {
        return Size.of(font.width(string), font.lineHeight)
    }

    fun measureString(string: Component): Size {
        return Size.of(font.width(string), font.lineHeight)
    }

    fun outline(size: Size, color: Int = contextColor) {
        guiGraphics.renderOutline(0, 0, size.width, size.height, color)
    }

    @ApiStatus.NonExtendable
    fun itemStackSize(): Size = ITEM_STACK_SIZE

    fun drawItemFilteredBg(itemStack: ItemStack, x: Int, y: Int) {
        val backend = SearchBackend.getCurrentBackend()
        val searchText = backend.getSearchString()
        if (searchText == null || searchText.isEmpty()) return
        if (!backend.matches(itemStack)) {
            guiGraphics.fill(x, y, x + 16, y + 16, 0xaf141414.toInt())
        } else {
            val color = 0xff00ff00.toInt()
            guiGraphics.fill(x, y, x + 1, y + 16, color)
            guiGraphics.fill(x, y, x + 16, y + 1, color)
            guiGraphics.fill(x + 15, y, x + 16, y + 16, color)
            guiGraphics.fill(x, y + 15, x + 16, y + 16, color)
        }
    }

    fun item(itemStack: ItemStack, x: Int = 0, y: Int = 0) {
        guiGraphics.renderItem(itemStack, x, y)
    }

    fun itemFiltered(itemStack: ItemStack, x: Int = 0, y: Int = 0) {
        drawItemFilteredBg(itemStack, x, y)
        item(itemStack, x, y)
    }

    fun itemDecoration(itemStack: ItemStack, x: Int = 0, y: Int = 0) {
        guiGraphics.renderItemDecorations(font, itemStack, x, y)
    }

    fun itemWithDecoration(itemStack: ItemStack, x: Int = 0, y: Int = 0) {
        item(itemStack, x, y)
        itemDecoration(itemStack, x, y)
    }

    fun itemWithFilteredDecoration(itemStack: ItemStack, x: Int = 0, y: Int = 0) {
        drawItemFilteredBg(itemStack, x, y)
        itemWithDecoration(itemStack, x, y)
    }

    /**
     * check if the mouse is in current size
     *
     * coordinate is based on the current [poseMatrix]
     */
    fun checkMouseInSize(size: Size): Boolean {
        if (size == Size.ZERO) return false

        val (mouseX, mouseY) = MousePositionManager

        val pose = guiGraphics.pose().last().pose()
        val checkVector = Vector4f(0f, 0f, 0f, 1f)
        pose.transform(checkVector)
        run {
            val x = checkVector.x
            val y = checkVector.y
            if (x >= mouseX || y >= mouseY) return false
        }

        checkVector.set(size.width.toFloat(), size.height.toFloat(), 0f, 1f)
        pose.transform(checkVector)
        val x = checkVector.x
        val y = checkVector.y
        return x > mouseX && y > mouseY
    }

    /**
     * @param outRadius the radius of the circle
     * @param colorOut the outer side color of the circle
     * @param colorIn the center side color of the circle
     * @param beginRadian from down, anti-cock wise grows
     * @param endRadian from down, anti-cock wise grows
     * @param tessellationCount the amount of division
     * @param isClockWise which side between [beginRadian] and [outRadius] will be filled
     */
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
                    matrix, sin(radius).toFloat() * outRadius, cos(radius).toFloat() * outRadius, 0.0f
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

    /**
     * @param inRadius the inner radius of the torus
     * @param outRadius the outer radius of the torus
     * @param colorOut the outer side color of the circle
     * @param colorIn the center side color of the circle
     * @param beginRadian from down, anti-cock wise grows
     * @param endRadian from down, anti-cock wise grows
     * @param tessellationCount the amount of division
     * @param isClockWise which side between [beginRadian] and [outRadius] will be filled
     */
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
        /**
         * a const indicates the length of [net.minecraft.world.item.ItemStack]
         */
        const val ITEM_STACK_LENGTH = 16
        val ITEM_STACK_SIZE = Size.of(ITEM_STACK_LENGTH, ITEM_STACK_LENGTH)
    }

    class DefaultStyle(override val guiGraphics: GuiGraphics) : HologramStyle {
        companion object {
            val SINGLE_INNER_PADDING = Padding(2)
            val COLLAPSE_SIZE = Size.of(4)
            const val SELECTED_COLOR = -1
            const val OUTLINE_COLOR = 0xff7f7f7f.toInt()
        }

        override var contextColor: Int = (0xff000000).toInt()

        override fun drawFullyBackground(size: Size) {
            val color = Config.Style.widgetBackgroundAlpha.get() shl 24 or 0x00ffffff
            this.fill(0, 0, size.width, size.height, color)
        }

        override fun drawOutlineSelected(size: Size) {
            outline(size, SELECTED_COLOR)
        }

        override fun mergeOutlineSizeForSingle(contentSize: Size): Size {
            return contentSize.expand(SINGLE_INNER_PADDING)
        }

        override fun mergeOutlineSizeForGroup(contentSize: Size, descriptionSize: Size, collapse: Boolean): Size {
            if (collapse) {
                require(contentSize == Size.ZERO)
                val width = descriptionSize.width + SINGLE_INNER_PADDING.horizontal * 2 + COLLAPSE_SIZE.width
                val height = descriptionSize.height + SINGLE_INNER_PADDING.vertical
                return Size.of(width, height)
            } else {
                val width = max(
                    contentSize.width,
                    descriptionSize.width + SINGLE_INNER_PADDING.horizontal + COLLAPSE_SIZE.width
                ) + SINGLE_INNER_PADDING.horizontal
                val height = contentSize.height + descriptionSize.height + SINGLE_INNER_PADDING.up
                return Size.of(width, height)
            }
        }

        override fun drawSingleOutline(size: Size, color: Int) {
            if (this.checkMouseInSize(size)) {
                stack {
                    drawOutlineSelected(size)
                }
            }
        }

        override fun drawGroupOutline(
            isGroupGlobal: Boolean,
            size: Size,
            descriptionSize: Size,
            collapse: Boolean,
            color: Int
        ) {

            outline(size, OUTLINE_COLOR)

            //draw collapse indicator
            val matrix = guiGraphics.pose().last().pose()
            val consumer = guiGraphics.bufferSource.getBuffer(RenderType.gui())
            run {
                val centerY = (descriptionSize.height / 2.0f) + SINGLE_INNER_PADDING.up
                val left = SINGLE_INNER_PADDING.left.toFloat()
                val right = left + COLLAPSE_SIZE.width.toFloat()
                val centerX = (left + right) / 2.0f
                val up = centerY - 2
                val down = centerY + 2
                val lineHalfWidth = 0.5f

                consumer.addVertex(matrix, left, (centerY - lineHalfWidth), 0.0f).setColor(color)
                consumer.addVertex(matrix, left, (centerY + lineHalfWidth), 0.0f).setColor(color)
                consumer.addVertex(matrix, right, (centerY + lineHalfWidth), 0.0f).setColor(color)
                consumer.addVertex(matrix, right, (centerY - lineHalfWidth), 0.0f).setColor(color)
                if (!collapse) {
                    consumer.addVertex(matrix, centerX - lineHalfWidth, up, 0.0f).setColor(color)
                    consumer.addVertex(matrix, centerX - lineHalfWidth, down, 0.0f).setColor(color)
                    consumer.addVertex(matrix, centerX + lineHalfWidth, down, 0.0f).setColor(color)
                    consumer.addVertex(matrix, centerX + lineHalfWidth, up, 0.0f).setColor(color)
                }
            }

            //draw split line
            if (isGroupGlobal && !collapse) {
                val left = SINGLE_INNER_PADDING.left * 2
                val right = size.width - SINGLE_INNER_PADDING.right * 2
                val y = max(descriptionSize.height, COLLAPSE_SIZE.height) + SINGLE_INNER_PADDING.vertical / 2 + 1
                this.drawHorizontalLine(left, right, y, 0xff000000.toInt())
            }
        }

        override fun moveToGroupDescription(descriptionSize: Size) {
            move(SINGLE_INNER_PADDING.horizontal + COLLAPSE_SIZE.width, SINGLE_INNER_PADDING.up)
            if (Config.Client.renderWidgetDebugInfo.get()) {
                stack {
                    pose().translate(0f, 0f, 100f)
                    outline(descriptionSize, 0xff00000ff.toInt())
                }
            }
        }

        override fun moveAfterDrawGroupOutline(descriptionSize: Size) {
            move(SINGLE_INNER_PADDING.left, descriptionSize.height + SINGLE_INNER_PADDING.vertical)
        }

        override fun moveAfterDrawSingleOutline() {
            move(SINGLE_INNER_PADDING.left, SINGLE_INNER_PADDING.up)
        }

        override fun elementPadding(): Int = 2
    }
}
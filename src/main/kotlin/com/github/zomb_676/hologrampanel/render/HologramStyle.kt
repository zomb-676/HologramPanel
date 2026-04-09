package com.github.zomb_676.hologrampanel.render

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.render.state.CircleRenderState
import com.github.zomb_676.hologrampanel.render.state.FloatColoredRectangleRenderState.Companion.fill
import com.github.zomb_676.hologrampanel.render.state.TorusRenderState
import com.github.zomb_676.hologrampanel.util.*
import com.github.zomb_676.hologrampanel.util.MousePositionManager.component1
import com.github.zomb_676.hologrampanel.util.MousePositionManager.component2
import com.github.zomb_676.hologrampanel.util.packed.AlignedScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import org.jetbrains.annotations.ApiStatus
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fStack
import org.joml.Vector3f
import kotlin.math.max

/**
 * a wrapped [GuiGraphicsExtractor] which is intended for convenience use and customize widget style
 */
interface HologramStyle {
    var guiGraphics: GuiGraphicsExtractor// TODO: Changing from val to var may potentially break things
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

    fun drawString(string: String, x: Int = 0, y: Int = 0, color: Int = ARGB.opaque(DyeColor.BLACK.textColor)) {
        guiGraphics.text(font, string, x, y, color, false)
    }

    fun drawString(string: Component, x: Int = 0, y: Int = 0, color: Int = ARGB.opaque(DyeColor.BLACK.textColor)) {
        guiGraphics.text(font, string, x, y, color, false)
    }

    fun drawString(string: FormattedCharSequence, x: Int = 0, y: Int = 0, color: Int = ARGB.opaque(DyeColor.BLACK.textColor)) {
        guiGraphics.text(font, string, x, y, color, false)
    }

    fun drawHorizontalLine(left: Int, right: Int, y: Int, color: Int = contextColor) {
        guiGraphics.horizontalLine(left, right, y, color)
    }

    fun drawVerticalLine(up: Int, down: Int, x: Int, color: Int = contextColor) {
        guiGraphics.verticalLine(x, up, down, color)
    }

    fun move(x: Int, y: Int) {
        this.translate(x.toFloat(), y.toFloat())
    }

    fun move(alignedScreenPosition: AlignedScreenPosition) {
        this.move(alignedScreenPosition.x, alignedScreenPosition.y)
    }

    fun translate(x: Float, y: Float) {
        pose().translate(x, y)
    }

    fun translate(x: Double, y: Double) {
        translate(x.toFloat(), y.toFloat())
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
        guiGraphics.fill(minX, minY, maxX, maxY, color)
    }

    /**
     * @param z by default, is 1.0
     */
    fun scale(x: Double, y: Double) {
        this.scale(x.toFloat(), y.toFloat())
    }

    /**
     * @param z by default, is 1.0
     */
    fun scale(x: Float, y: Float) {
        guiGraphics.pose().scale(x, y)
    }

    /**
     * not scale z
     */
    fun scale(scale: Double) {
        this.scale(scale, scale)
    }

    /**
     * not scale z
     */
    fun scale(scale: Float) {
        this.scale(scale, scale)
    }

    fun push(): Matrix3x2fStack = this.guiGraphics.pose().pushMatrix()
    fun pop(): Matrix3x2fStack = this.guiGraphics.pose().popMatrix()

    fun pose(): Matrix3x2fStack = this.guiGraphics.pose()

    val font: Font get() = Minecraft.getInstance().font

    fun measureString(string: String): Size {
        return Size.of(font.width(string), font.lineHeight)
    }

    fun measureString(string: Component): Size {
        return Size.of(font.width(string), font.lineHeight)
    }

    fun outline(size: Size, color: Int = contextColor) {
        guiGraphics.outline(0, 0, size.width, size.height, color)
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
        guiGraphics.item(itemStack, x, y)
    }

    fun itemFiltered(itemStack: ItemStack, x: Int = 0, y: Int = 0) {
        drawItemFilteredBg(itemStack, x, y)
        item(itemStack, x, y)
    }

    fun itemDecoration(itemStack: ItemStack, x: Int = 0, y: Int = 0) {
        guiGraphics.itemDecorations(font, itemStack, x, y)
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
     * coordinate is based on the current [pose]
     */
    fun checkMouseInSize(size: Size): Boolean {
        if (size == Size.ZERO) return false

        val (mouseX, mouseY) = MousePositionManager

        val pose = guiGraphics.pose()
        val checkVector = Vector3f(0f, 0f, 1f)
        pose.transform(checkVector)
        run {
            val x = checkVector.x
            val y = checkVector.y
            if (x >= mouseX || y >= mouseY) return false
        }

        checkVector.set(size.width.toFloat(), size.height.toFloat(), 1f)
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

        // VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR
        guiGraphics.submitGuiElementRenderState(CircleRenderState(
            AllRegisters.RenderPipelines.CIRCLE,
            TextureSetup.noTexture(),
            Matrix3x2f(guiGraphics.pose()),
            outRadius,
            beginRadian,
            endRadian,
            tessellationCount,
            colorIn,
            colorOut,
            guiGraphics.peekScissorStack()
        ))
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

        guiGraphics.submitGuiElementRenderState(TorusRenderState(
            AllRegisters.RenderPipelines.STRIP_BASIC,
            TextureSetup.noTexture(),
            Matrix3x2f(guiGraphics.pose()),
            inRadius,
            outRadius,
            beginRadian,
            endRadian,
            tessellationCount,
            colorIn,
            colorOut,
            guiGraphics.peekScissorStack()
        ))
    }

    companion object {
        /**
         * a const indicates the length of [ItemStack]
         */
        const val ITEM_STACK_LENGTH = 16
        val ITEM_STACK_SIZE = Size.of(ITEM_STACK_LENGTH, ITEM_STACK_LENGTH)
    }

    class DefaultStyle(override var guiGraphics: GuiGraphicsExtractor) : HologramStyle {
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
            run {
                val centerY = (descriptionSize.height / 2.0f) + SINGLE_INNER_PADDING.up
                val left = SINGLE_INNER_PADDING.left.toFloat()
                val right = left + COLLAPSE_SIZE.width.toFloat()
                val centerX = (left + right) / 2.0f
                val up = centerY - 2
                val down = centerY + 2
                val lineHalfWidth = 0.5f

                guiGraphics.fill(left, centerY - lineHalfWidth, right, centerY + lineHalfWidth, color)
                if (!collapse) {
                    guiGraphics.fill(centerX - lineHalfWidth, up, centerX + lineHalfWidth, down, color)
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
                outline(descriptionSize, 0xff00000ff.toInt())
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
package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.render.HologramStyle.Companion.ITEM_STACK_LENGTH
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.ScreenPosition
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.stack
import com.mojang.blaze3d.platform.Lighting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.ClientHooks
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
import net.neoforged.neoforge.client.textures.FluidSpriteCache
import net.neoforged.neoforge.fluids.FluidType
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

interface IRenderElement {

    fun measureContentSize(style: HologramStyle): Size
    fun render(style: HologramStyle, partialTicks: Float)

    fun setScale(scale: Double): IRenderElement
    fun getScale(): Double

    fun setPositionOffset(x: Int, y: Int): IRenderElement
    fun getPositionOffset(): ScreenPosition

    var contentSize: Size

    companion object {
        private fun Float.resetNan() = if (this.isNaN()) 0.0f else this

        fun shortDescription(value: Float) = when {
            value < 1e3 -> "%.2f".format(value)
            value < 1e6 -> "%.2fK".format(value / 1e3)
            value < 1e9 -> "%.2fM".format(value / 1e6)
            else -> "%.2fB".format(value / 1e9)
        }
    }

    data object EmptyElement : IRenderElement {
        override fun measureContentSize(style: HologramStyle): Size = Size.ZERO

        override fun render(
            style: HologramStyle, partialTicks: Float
        ) {
        }

        override fun setScale(scale: Double): IRenderElement = this
        override fun getScale(): Double = 1.0

        override fun setPositionOffset(x: Int, y: Int): IRenderElement = this

        override fun getPositionOffset(): ScreenPosition = ScreenPosition.ZERO

        override var contentSize: Size
            get() = Size.ZERO
            set(value) {}
    }

    abstract class RenderElement : IRenderElement {
        final override var contentSize: Size = Size.ZERO

        private var scale: Double = 1.0
            @JvmName("privateScaleSet") set(value) {
                require(value > 0)
                field = value
            }

        private var positionOffset: ScreenPosition = ScreenPosition.ZERO

        protected fun Size.scale(): Size {
            if (scale == 1.0) {
                return this
            } else {
                val w = floor(this.width * scale).toInt()
                val h = floor(this.height * scale).toInt()
                return Size.of(w, h)
            }
        }

        final override fun getPositionOffset(): ScreenPosition = this.positionOffset

        final override fun setPositionOffset(x: Int, y: Int): RenderElement {
            this.positionOffset = ScreenPosition.of(x, y)
            return this
        }

        final override fun getScale(): Double = scale

        final override fun setScale(scale: Double): RenderElement {
            this.scale = scale
            return this
        }
    }

    open class EntityRenderElement(val entity: Entity, val entityScale: Double) : RenderElement() {
        companion object {
            val QUATERNION: Quaternionf = Quaternionf().rotateZ(Math.PI.toFloat())
        }

        override fun measureContentSize(style: HologramStyle): Size {
            return Size.of(
                floor(entity.bbWidth * entityScale * 2).toInt(), floor(entity.bbHeight * entityScale).toInt()
            ).expandWidth(2).expandHeight(2).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            val guiGraphics = style.guiGraphics
            style.stack {
                guiGraphics.pose().translate(
                    contentSize.width.toFloat() / 2, contentSize.height.toFloat(), 50.0f
                )
                val entityScale = entityScale.toFloat()
                guiGraphics.pose().scale(entityScale, entityScale, -entityScale)
                guiGraphics.pose()
                    .mulPose(Quaternionf().rotateXYZ(Math.toRadians(15.0).toFloat(), 0f, Math.PI.toFloat()))
                guiGraphics.flush()
                Lighting.setupForEntityInInventory()
                val dispatcher = Minecraft.getInstance().entityRenderDispatcher

                dispatcher.setRenderShadow(false)
                dispatcher.render(
                    entity, 0.0, 0.0, 0.0, 1.0f, guiGraphics.pose(), guiGraphics.bufferSource, LightTexture.FULL_BRIGHT
                )
                guiGraphics.flush()
                dispatcher.setRenderShadow(true)
                Lighting.setupFor3DItems()
            }
        }

        protected fun renderEntityOutline(style: HologramStyle) {
            val colorWhite = -1
            val graphics = style.guiGraphics
            val centerX = contentSize.width / 2
            val halfWidth = entity.bbWidth * entityScale * getScale()
            val height = entity.bbHeight * entityScale * getScale()
            graphics.hLine(-1000, 1000, contentSize.height, colorWhite)
            graphics.hLine(-1000, 1000, (contentSize.height - height).toInt(), colorWhite)
            graphics.vLine((centerX - halfWidth).toInt(), -1000, +1000, colorWhite)
            graphics.vLine((centerX + halfWidth).toInt(), -1000, +1000, colorWhite)
        }

    }

    @Deprecated("use the entity variant", replaceWith = ReplaceWith("EntityRenderElement"), DeprecationLevel.HIDDEN)
    open class LivingEntityRenderElement(val entity: LivingEntity, val entityScale: Double) : RenderElement() {
        companion object {
            val QUATERNION: Quaternionf = Quaternionf().rotateZ(Math.PI.toFloat())
            val EMPTY_VECTOR = Vector3f()
        }

        override fun measureContentSize(style: HologramStyle): Size {
            return Size.of(
                floor(entity.bbWidth * entityScale * 2).toInt(), floor(entity.bbHeight * entityScale).toInt()
            ).expandWidth(2).expandHeight(2).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            InventoryScreen.renderEntityInInventory(
                style.guiGraphics,
                contentSize.width.toFloat() / 2,
                contentSize.height.toFloat(),
                entityScale.toFloat(),
                EMPTY_VECTOR,
                QUATERNION,
                null,
                entity
            )
        }

        protected fun renderEntityOutline(style: HologramStyle) {
            val colorWhite = -1
            val graphics = style.guiGraphics
            val centerX = contentSize.width / 2
            val halfWidth = entity.bbWidth * entityScale * getScale()
            val height = entity.bbHeight * entityScale * getScale()
            graphics.hLine(-1000, 1000, contentSize.height, colorWhite)
            graphics.hLine(-1000, 1000, (contentSize.height - height).toInt(), colorWhite)
            graphics.vLine((centerX - halfWidth).toInt(), -1000, +1000, colorWhite)
            graphics.vLine((centerX + halfWidth).toInt(), -1000, +1000, colorWhite)
        }

    }

    open class StringRenderElement(val component: Component) : RenderElement() {
        constructor(string: String) : this(Component.literal(string))

        override fun measureContentSize(style: HologramStyle): Size {
            return style.measureString(component).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            style.drawString(component)
        }
    }

    open class ScreenTooltipElement(val item: ItemStack) : RenderElement() {
        var sprite = item.get(DataComponents.TOOLTIP_STYLE)
        var tooltips: List<ClientTooltipComponent> = listOf()
        override fun measureContentSize(style: HologramStyle): Size {
            sprite = item.get(DataComponents.TOOLTIP_STYLE)
            val window = Minecraft.getInstance().window
            tooltips = ClientHooks.gatherTooltipComponents(
                item,
                Screen.getTooltipFromItem(Minecraft.getInstance(), item),
                item.tooltipImage,
                window.guiScaledWidth / 2,
                window.guiScaledWidth,
                window.guiScaledHeight,
                style.font
            )
            var width = 0
            var height = if (tooltips.size == 1) -1 else 0
            tooltips.forEach { tooltip ->
                width = max(width, tooltip.getWidth(style.font))
                height += tooltip.getHeight(style.font)
            }
            return Size.of(width + 6, height + 6).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            val texture = ClientHooks.onRenderTooltipTexture(
                item,
                style.guiGraphics,
                0,
                0,
                style.font,
                tooltips,
                sprite
            )
            var height = 0
            style.stack {
                style.guiGraphics.pose().translate(2.0, 2.0, 400.0)
                TooltipRenderUtil.renderTooltipBackground(
                    style.guiGraphics,
                    0,
                    0,
                    contentSize.width - 4,
                    contentSize.height - 5,
                    0,
                    texture.texture
                )
                tooltips.forEachIndexed { index, tooltip ->
                    tooltip.renderText(
                        style.font,
                        0,
                        height,
                        style.guiGraphics.pose().last().pose(),
                        style.guiGraphics.bufferSource
                    )
                    height += tooltip.getHeight(style.font)
                }
                height = 0
                tooltips.forEachIndexed { index, tooltip ->
                    tooltip.renderImage(
                        style.font,
                        0,
                        height,
                        contentSize.width, contentSize.height, style.guiGraphics
                    )
                    height += tooltip.getHeight(style.font)
                }
            }
        }

    }

    open class ItemStackElement(val renderDecoration: Boolean = true, val itemStack: ItemStack) : RenderElement() {

        override fun measureContentSize(style: HologramStyle): Size = style.itemStackSize().scale()

        override fun render(
            style: HologramStyle, partialTicks: Float
        ) {
            style.guiGraphics.renderItem(itemStack, 0, 0)
            if (renderDecoration) {
                style.guiGraphics.renderItemDecorations(style.font, itemStack, 0, 0)
            }
        }

        fun smallItem(): ItemStackElement {
            this.setScale(0.5)
            return this
        }
    }

    open class ItemsElement(val items: List<ItemStack>) : RenderElement() {
        companion object {
            const val ITEM_EACH_LINE = 6
            const val PADDING = 1
        }

        val count = items.size
        val lineCount = ceil(items.size.toDouble() / ITEM_EACH_LINE).toInt()

        override fun measureContentSize(style: HologramStyle): Size {
            val l = ITEM_STACK_LENGTH
            val height = l * lineCount + PADDING * (lineCount + 1)
            val width = if (lineCount > 1) {
                l * ITEM_EACH_LINE + PADDING * (ITEM_EACH_LINE + 1)
            } else {
                l * count + PADDING * (count + 1)
            }
            return Size.of(width, height).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            val font = style.font
            style.outline(this.contentSize)
            style.stack {
                style.move(PADDING, PADDING)
                style.push()
                var i = 0
                items.forEachIndexed { index, item ->
                    if (i == ITEM_EACH_LINE) {
                        i = 0
                        style.pop()
                        style.move(0, ITEM_STACK_LENGTH + PADDING)
                        style.push()
                    }
                    style.guiGraphics.renderItem(item, 0, 0)
                    style.guiGraphics.renderItemDecorations(font, item, 0, 0)
                    style.move(ITEM_STACK_LENGTH + PADDING, 0)
                    i++
                }
                style.pop()
            }
        }

    }

    class TextureAtlasSpriteRenderElement(val sprite: TextureAtlasSprite) : RenderElement() {
        companion object {
            @Suppress("DEPRECATION")
            val missing: TextureAtlasSprite =
                Minecraft.getInstance().modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(MissingTextureAtlasSprite.getLocation())
        }

        private var width = sprite.contents().width()
        private var height = sprite.contents().height()

        override fun measureContentSize(style: HologramStyle): Size {
            return Size.of(width, height).scale()
        }

        override fun render(
            style: HologramStyle, partialTicks: Float
        ) {
            val size = this.contentSize
            style.guiGraphics.blitSprite(RenderType::guiTextured, sprite, 0, 0, size.width, size.height)
        }

        fun setRenderSize(width: Int, height: Int) {
            this.width = width
            this.height = height
        }
    }

    abstract class ProgressBarElement(val progress: ProgressData, var barWidth: Float = 98f) : RenderElement() {

        override fun measureContentSize(
            style: HologramStyle
        ): Size {
            return Size.of(floor(barWidth).toInt() + 2, style.font.lineHeight + 2).scale()
        }

        val decorateLineColor = 0xff555555.toInt()

        override fun render(style: HologramStyle, partialTicks: Float) {
            if (this.requireOutlineDecorate()) {
                style.outline(this.contentSize, style.contextColor)
            }

            val percent = progress.percent
            style.stack {
                if (this.requireOutlineDecorate()) {
                    style.move(1, 1)
                }
                val left: Float
                val right: Float
                val height: Float = if (this.requireOutlineDecorate()) {
                    this.contentSize.height - 2
                } else {
                    this.contentSize.height
                }.toFloat()
                if (progress.LTR) {
                    left = 0.0f
                    right = barWidth * percent
                } else {
                    left = (1.0f - percent) * barWidth
                    right = barWidth.toFloat()
                }
                this.fillBar(style, left, right, height, percent)
            }

            val description = getDescription(percent)
            val width = style.measureString(description).width
            style.stack {
                style.guiGraphics.pose().translate(0.0, 0.0, 1.0)
                style.drawString(description, (this.contentSize.width - width) / 2, 2)
            }
        }

        abstract fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float)
        open fun getDescription(percent: Float): Component = Component.literal("%.1f%%".format(percent * 100))

        open fun requireOutlineDecorate(): Boolean = false
    }

    class EnergyBarElement(progress: ProgressData, barWidth: Float = 60f) : ProgressBarElement(progress, barWidth) {
        override fun requireOutlineDecorate(): Boolean = true

        override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
            style.fill(left, 0f, right, height, 0xffFF6B6B.toInt())
            if (progress.LTR) {
                style.fill(right, 0f, barWidth, height, 0xafDDDDDD.toInt())
            } else {
                style.fill(0f, left, barWidth, height, 0xafDDDDDD.toInt())
            }
        }

        override fun getDescription(percent: Float): Component {
            val current = if (progress.progressCurrent < 1000) {
                "${progress.progressCurrent}"
            } else {
                shortDescription(progress.progressCurrent.toFloat() / 1000)
            }
            val f = if (progress.progressCurrent == progress.progressMax) {
                current
            } else {
                val max = if (progress.progressMax < 1000) {
                    "${progress.progressMax}"
                } else {
                    shortDescription(progress.progressMax.toFloat() / 1000)
                }
                "$current/$max"
            }
            return Component.literal("").append(f).append("FE")
        }
    }

    class FluidBarElement(progress: ProgressData, val fluid: FluidType) : ProgressBarElement(progress) {
        override fun requireOutlineDecorate(): Boolean = true

        override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
            val left = left.toFloat()
            val right = right.toFloat()
            val height = height.toFloat()

            val handle: IClientFluidTypeExtensions = IClientFluidTypeExtensions.of(fluid)
            val tintColor = handle.tintColor
            val sprite: TextureAtlasSprite = FluidSpriteCache.getSprite(handle.stillTexture)

            val matrix = style.guiGraphics.pose().last().pose()
            val consumer = style.guiGraphics.bufferSource.getBuffer(RenderType.guiTextured(sprite.atlasLocation()))

            val maxU = (((sprite.u1 - sprite.u0) * percent) + sprite.u0).toFloat()
            val maxV = (((sprite.v1 - sprite.v0) * percent) + sprite.v0).toFloat()

            consumer.addVertex(matrix, left, 0f, 0f).setUv(sprite.u0, sprite.v0).setColor(tintColor)
            consumer.addVertex(matrix, left, height, 0f).setUv(sprite.u0, maxV).setColor(tintColor)
            consumer.addVertex(matrix, right, height, 0f).setUv(maxU, maxV).setColor(tintColor)
            consumer.addVertex(matrix, right, 0f, 0f).setUv(maxU, sprite.v0).setColor(tintColor)
        }

        override fun getDescription(percent: Float): Component {
            val current = if (progress.progressCurrent < 1000) {
                "${progress.progressCurrent}mB"
            } else {
                "${shortDescription(progress.progressCurrent.toFloat() / 1000)}B"
            }
            val f = if (progress.progressCurrent == progress.progressMax) {
                current
            } else {
                val max = if (progress.progressMax < 1000) {
                    "${progress.progressMax}mB"
                } else {
                    "${shortDescription(progress.progressMax.toFloat() / 1000)}B"
                }
                "$current/$max"
            }
            val fluidName = fluid.description
            return Component.literal("").append(fluidName).append(" ").append(f)
        }
    }

    class WorkingArrowProgressBarElement(progress: ProgressData, barWidth: Float = 15f) :
        ProgressBarElement(progress, barWidth) {

        override fun render(style: HologramStyle, partialTicks: Float) {
            val percent = progress.percent.resetNan()
            style.stack {
                style.move(1, 1)
                val height = (this.contentSize.height - 2).toFloat()
                if (progress.LTR) {
                    this.fillBar(style, 0.0f, barWidth * percent, height, percent)
                } else {
                    this.fillBar(style, (1.0f - percent) * barWidth, barWidth, height, percent)
                }
            }
        }

        companion object {
            const val AXIAL_HALF_WIDTH = 1.75f
            const val BASE_COLOR = 0xff5b5b5b.toInt()
            const val FILL_COLOR = -1
        }

        override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
            val left = left.toFloat()
            val right = right.toFloat()
            val height = height.toFloat()
            style.guiGraphics.flush()

            val buffer = style.guiGraphics.bufferSource
            val consumer = buffer.getBuffer(RenderType.gui())
            val width = this.contentSize.width.toFloat()

            val pose = style.guiGraphics.pose().last().pose()

            val halfHeight = height / 2
            val cuttingPointX = width - (height / 2.0f)
            //draw base
            consumer.addVertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, cuttingPointX, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)

            consumer.addVertex(pose, width, height / 2, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, cuttingPointX, 0.0f, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, cuttingPointX, height, 0.0f).setColor(BASE_COLOR)
            //another for quad
            consumer.addVertex(pose, cuttingPointX, height, 0.0f).setColor(BASE_COLOR)

            //draw fill
            if (right <= cuttingPointX) {
                consumer.addVertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, right, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, right, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
            } else {
                consumer.addVertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, cuttingPointX, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)

                consumer.addVertex(pose, cuttingPointX, 0.0f, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, cuttingPointX, height, 0.0f).setColor(FILL_COLOR)
                val remain = width - right
                consumer.addVertex(pose, right, halfHeight + remain, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, right, halfHeight - remain, 0.0f).setColor(FILL_COLOR)
            }
        }
    }

    class WorkingCircleProgressElement(val progress: ProgressData, val outRadius: Float, val inRadius: Float) :
        RenderElement() {
        override fun measureContentSize(style: HologramStyle): Size {
            return Size.of(floor(outRadius * 2).toInt())
        }

        companion object {
            const val BASE_COLOR = 0xff88abdc.toInt()
            const val FILL_COLOR = 0xff4786da.toInt()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            val percent = progress.percent.resetNan()

            style.stack {
                val move = this.contentSize.width / 2f
                style.translate(move, move)
                val end = percent * Math.PI * 2
                if (inRadius > 0.01f) {
                    style.drawTorus(inRadius, outRadius, FILL_COLOR, beginRadian = Math.PI, endRadian = Math.PI - end)
                } else {
                    style.drawCycle(outRadius, BASE_COLOR, beginRadian = Math.PI * 2, endRadian = 0.0)
                    style.drawCycle(outRadius, FILL_COLOR, beginRadian = Math.PI, endRadian = Math.PI - end)
                }
            }
        }
    }
}
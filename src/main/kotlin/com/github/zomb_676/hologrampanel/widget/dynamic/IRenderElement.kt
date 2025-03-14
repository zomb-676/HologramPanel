package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ScreenPosition
import com.github.zomb_676.hologrampanel.util.SelectPathType
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.stack
import com.mojang.blaze3d.platform.Lighting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
import net.neoforged.neoforge.client.textures.FluidSpriteCache
import net.neoforged.neoforge.fluids.FluidType
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL46
import kotlin.math.floor
import kotlin.math.min

interface IRenderElement {

    fun measureContentSize(style: HologramStyle): Size
    fun render(style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType)

    fun setScale(scale: Double): RenderElement
    fun getScale(): Double

    fun setPositionOffset(x: Int, y: Int): RenderElement
    fun getPositionOffset(): ScreenPosition

    var contentSize: Size

    companion object {
        private fun Float.resetNan() = if (this.isNaN()) 0.0f else this
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

        override fun render(style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType) {
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

        override fun render(style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType) {
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

        override fun measureContentSize(style: HologramStyle): Size {
            return style.measureString(component).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType) {
            style.drawString(component)
        }
    }

    open class ItemStackElement(val renderDecoration: Boolean = true, val itemStack: ItemStack) : RenderElement() {

        override fun measureContentSize(style: HologramStyle): Size = style.itemStackSize()

        override fun render(
            style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType
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
            style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType
        ) {
            val size = this.contentSize
            style.guiGraphics.blitSprite(RenderType::guiTextured, sprite, 0, 0, size.width, size.height)
        }

        fun setRenderSize(width: Int, height: Int) {
            this.width = width
            this.height = height
        }
    }

    class ProgressData(var progressCurrent: Int = 0, var progressMax: Int = 100, val LTR: Boolean = true) {
        val percent get() = progressCurrent.toFloat() / progressMax

        fun current(value: Int): ProgressData {
            this.progressCurrent = value
            return this
        }

        fun max(value: Int): ProgressData {
            this.progressMax = value
            return this
        }
    }

    abstract class ProgressBarElement(val progress: ProgressData, var barWidth: Float = 35f) : RenderElement() {

        override fun measureContentSize(
            style: HologramStyle
        ): Size {
            return Size.of(floor(barWidth).toInt() + 2, style.font.lineHeight + 2).scale()
        }

        val decorateLineColor = 0xff555555.toInt()

        override fun render(style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType) {
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
                if (this.requireLineDecorate()) {
                    this.lineDecorate(style, left, right, height, percent)
                }
            }

            val description = getDescription(percent)
            val width = style.measureString(description).width
            style.drawString(description, (this.contentSize.width - width) / 2, 2)
        }

        abstract fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float)
        open fun getDescription(percent: Float): String = "%.1f%%".format(percent * 100)

        open fun requireLineDecorate(): Boolean = false
        open fun requireOutlineDecorate(): Boolean = false

        fun lineDecorate(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
            if (progress.LTR) {
                var xx = left + 1
                while (xx < right) {
                    style.fill(xx, 0f, min(xx + 1, right), height, decorateLineColor)
                    xx += 2.0f
                }
            } else {
                var xx = right - 1
                while (xx > left) {
                    style.fill(xx, 0f, min(xx - 1, left), height, decorateLineColor)
                    xx -= 2.0f
                }
            }
        }

    }

    class EnergyBarElement(progress: ProgressData) : ProgressBarElement(progress) {
        override fun requireLineDecorate(): Boolean = true
        override fun requireOutlineDecorate(): Boolean = true

        override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
            style.fill(left, 0f, right, height, 0xffFF5555.toInt())
        }

//        override fun getDescription(): String = "${progress.progressCurrent}/${progress.progressMax}"
    }

    class FluidBarElement(progress: ProgressData, val fluid: FluidType) : ProgressBarElement(progress) {
        override fun requireLineDecorate(): Boolean = false
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

        override fun getDescription(progress: Float): String = "1"
    }

    class WorkingArrowProgressBarElement(progress: ProgressData, barWidth: Float = 15f) :
        ProgressBarElement(progress, barWidth) {

        override fun render(style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType) {
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

        override fun render(style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType) {
            val percent = progress.percent.resetNan()

            GL46.glPushDebugGroup(GL46.GL_DEBUG_SOURCE_APPLICATION, 0, "widget")
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
            GL46.glPopDebugGroup()
        }
    }
}
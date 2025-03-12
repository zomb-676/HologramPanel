package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ScreenPosition
import com.github.zomb_676.hologrampanel.util.SelectPathType
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.stack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
import net.neoforged.neoforge.client.textures.FluidSpriteCache
import net.neoforged.neoforge.fluids.FluidStack
import kotlin.math.floor

interface IRenderElement {

    fun measureContentSize(style: HologramStyle): Size
    fun render(style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType)

    fun setScale(scale: Double): RenderElement
    fun getScale(): Double

    fun setPositionOffset(x: Int, y: Int): RenderElement
    fun getPositionOffset(): ScreenPosition

    var contentSize: Size

    abstract class RenderElement : IRenderElement {
        final override var contentSize: Size = Size.ZERO

        private var scale: Double = 1.0
            @JvmName("privateScaleSet")
            set(value) {
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

    open class StringRenderElement(val component: Component) : RenderElement() {

        override fun measureContentSize(
            style: HologramStyle
        ): Size {
            return style.measureString(component).scale()
        }

        override fun render(
            style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType
        ) {
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

        override fun measureContentSize(style: HologramStyle): Size {
            val contents = sprite.contents()
            return Size.of(contents.width(), contents.height()).scale()
        }

        override fun render(
            style: HologramStyle,
            partialTicks: Float,
            forTerminal: SelectPathType
        ) {
            val size = this.contentSize
            style.guiGraphics.blitSprite(RenderType::guiTextured, sprite, 0, 0, size.width, size.height)
        }
    }

    class ProgressData(var progressCurrent: Int = 0, var progressMax: Int = 100, val LTR: Boolean = true) {
        val percent get() = progressCurrent.toDouble() / progressMax

        fun current(value : Int): ProgressData {
            this.progressCurrent = value
            return this
        }

        fun max(value : Int): ProgressData {
            this.progressMax = value
            return this
        }
    }

    abstract class ProgressBarElement(val progress: ProgressData) : RenderElement() {
        companion object {
            const val BAR_WITH = 30
        }

        override fun measureContentSize(
            style: HologramStyle
        ): Size {
            return Size.of(BAR_WITH + 2, style.font.lineHeight + 4).scale()
        }

        override fun render(
            style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType
        ) {
            style.guiGraphics.renderOutline(
                0, 0, this.contentSize.width, this.contentSize.height, style.contextColor
            )
            val description = getDescription()
            val width = style.measureString(description).width
            style.drawString(description, (this.contentSize.width - width) / 2, 2)

            style.stack {
                style.move(1, 1)
                if (progress.LTR) {
                    this.fillBar(style, 0, (BAR_WITH * progress.percent).toInt(), 11)
                } else {
                    this.fillBar(style, ((1.0 - progress.percent) * BAR_WITH).toInt(), BAR_WITH, 11)
                }
            }
        }

        abstract fun fillBar(style: HologramStyle, left: Int, right: Int, height: Int)
        open fun getDescription(): String = java.lang.String.valueOf(progress.percent * 100) + "%"
    }

    class EnergyBarElement(progress: ProgressData) : ProgressBarElement(progress) {
        override fun fillBar(style: HologramStyle, left: Int, right: Int, height: Int) {
            style.fill(left, 0, right, height, 0xffff0000.toInt())
        }

//        override fun getDescription(): String = "${progress.progressCurrent}/${progress.progressMax}"
    }

    class FluidBarElement(progress: ProgressData, val fluid: FluidStack) : ProgressBarElement(progress) {
        override fun fillBar(style: HologramStyle, left: Int, right: Int, height: Int) {
            val fluid = fluid ?: return
            val handle: IClientFluidTypeExtensions = IClientFluidTypeExtensions.of(fluid.fluidType)
            val still = handle.stillTexture
            val atlasSprite: TextureAtlasSprite = FluidSpriteCache.getSprite(still)
            val width = right - left
            style.guiGraphics.blitSprite(RenderType::guiTextured, atlasSprite, left, 0, width, height)
        }

        override fun getDescription(): String = "1"
    }

    class WorkingProgressBarElement(progress: ProgressData) : ProgressBarElement(progress) {
        override fun fillBar(
            style: HologramStyle,
            left: Int,
            right: Int,
            height: Int
        ) {

        }
    }
}
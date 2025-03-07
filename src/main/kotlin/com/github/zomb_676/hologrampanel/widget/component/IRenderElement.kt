package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ScreenPosition
import com.github.zomb_676.hologrampanel.util.SelectPathType
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.component.IRenderElement
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import kotlin.math.floor

interface IRenderElement {

    fun measureContentSize(style: HologramStyle): Size
    fun render(style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType)
    fun update()

    fun setScale(scale: Double): RenderElement
    fun getScale(): Double

    fun setPositionOffset(x: Int, y: Int): RenderElement
    fun getPositionOffset(): ScreenPosition

    var contentSize: Size

    class StaticRenderElement(val warp : IRenderElement) : IRenderElement by warp {
        private var hasUpdate = false
        override fun update() {
            if (hasUpdate) return
            warp.update()
            hasUpdate = true
        }

        companion object {
            fun <T : IRenderElement> tryWrap(warp : T) =
                warp as? StaticRenderElement ?: StaticRenderElement(warp)
        }
    }

    companion object {
        const val UN_LOAD_STRING = "N"
    }

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

    open class StringRenderElement(val updater: () -> Component) : RenderElement() {
        private var str: Component = Component.literal("un_initialized")

        override fun measureContentSize(
            style: HologramStyle
        ): Size {
            return style.measureString(str).scale()
        }

        override fun render(
            style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType
        ) {
            style.drawString(str)
        }

        override fun update() {
            this.str = updater.invoke()
        }
    }

    open class ItemStackElement(val renderDecoration: Boolean = true, val updater: () -> ItemStack) : RenderElement() {
        private var itemStack: ItemStack? = null

        override fun measureContentSize(style: HologramStyle): Size {
            return if (itemStack == null) {
                style.measureString(UN_LOAD_STRING)
            } else {
                style.itemStackSize()
            }.scale()
        }

        override fun render(
            style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType
        ) {
            if (itemStack == null) {
                style.drawString(UN_LOAD_STRING)
            } else {
                style.guiGraphics.renderItem(itemStack, 0, 0)
                if (renderDecoration) {
                    style.guiGraphics.renderItemDecorations(style.font, itemStack, 0, 0)
                }
            }
        }

        override fun update() {
            this.itemStack = updater.invoke()
        }

        fun smallItem(): ItemStackElement {
            this.setScale(0.5)
            return this
        }
    }

    class TextureAtlasSpriteRenderElement(val updater: () -> TextureAtlasSprite) : RenderElement() {
        companion object {
            @Suppress("DEPRECATION")
            val missing: TextureAtlasSprite =
                Minecraft.getInstance().modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(MissingTextureAtlasSprite.getLocation())
        }

        private var sprite: TextureAtlasSprite = missing

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

        override fun update() {
            this.sprite = updater.invoke()
        }
    }

    abstract class ProgressBarElement() : RenderElement() {
        var progressMax: Int = -1
            private set
        var progressCurrent: Int = -1
            private set
        var percent = 0.0
            private set

        abstract fun progressMax(): Int
        abstract fun progressCurrent(): Int
        open fun working(): Boolean = progressMax != 0
        open fun initialized() = progressMax >= 0 && progressCurrent >= 0

        open fun fromLeftToRight(): Boolean = true

        override fun update() {
            this.progressMax = progressMax()
            this.progressCurrent = progressCurrent()
            this.percent = progressCurrent.toDouble() / progressMax
        }

        override fun measureContentSize(
            style: HologramStyle
        ): Size {
            return Size.of(30, style.font.lineHeight).scale()
        }

        override fun render(
            style: HologramStyle, partialTicks: Float, forTerminal: SelectPathType
        ) {
            style.guiGraphics.renderOutline(
                0, 0, this.contentSize.width, this.contentSize.height, style.contextColor
            )
            style.moveAfterDrawSlotOutline()
            style.stack {
                this.fillBar(style, this.contentSize)
            }
            val description = getDescription()
            val width = style.measureString(description).width
            style.drawString(description, (this.contentSize.width - width) / 2, 0)
        }

        abstract fun fillBar(style: HologramStyle, size: Size)
        abstract fun getDescription(): String
    }

//    class EnergyBarElement : ProgressBarElement() {
//        override fun progressMax(target: IEnergyStorage): Int = target.maxEnergyStored
//        override fun progressCurrent(target: IEnergyStorage): Int = target.energyStored
//        override fun fillBar(style: HologramStyle, size: Size) {
//            style.fill(size, 0xffff0000.toInt())
//        }
//
//        override fun getDescription(): String = "$progressCurrent/$progressMax"
//    }
//    class FluidBarElement : ProgressBarElement() {
//        override fun progressMax(target: IFluidTank): Int = target.capacity
//        override fun progressCurrent(target: IFluidTank): Int = target.fluidAmount
//
//        private var fluid: FluidStack? = null
//
//        override fun update(target: IFluidTank) {
//            super.update(target)
//            this.fluid = target.fluid
//        }
//
//        override fun fillBar(
//            style: HologramStyle,
//            size: Size
//        ) {
//            val fluid = fluid ?: return
//            val handle: IClientFluidTypeExtensions = IClientFluidTypeExtensions.of(fluid.fluidType)
//            val still = handle.stillTexture
//            val atlasSprite: TextureAtlasSprite = FluidSpriteCache.getSprite(still)
//            style.guiGraphics.blitSprite(RenderType::guiTextured, atlasSprite, 0,0,size.width, size.height)
//        }
//
//        override fun getDescription(): String = "1"
//    }

    class WorkingProgressBarElement
    class FuelProgressBar
}
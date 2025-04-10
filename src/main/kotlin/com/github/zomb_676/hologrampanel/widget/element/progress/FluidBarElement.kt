package com.github.zomb_676.hologrampanel.widget.element.progress

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement.Companion.shortDescription
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
import net.neoforged.neoforge.client.textures.FluidSpriteCache
import net.neoforged.neoforge.fluids.FluidType

class FluidBarElement(progress: ProgressData, val fluid: FluidType) : ProgressBarElement(progress) {
    override fun requireOutlineDecorate(): Boolean = true

    override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
        val left = left.toFloat()
        val right = right.toFloat()
        val height = height.toFloat()

        val handle: IClientFluidTypeExtensions = IClientFluidTypeExtensions.of(fluid)
        val tintColor = handle.tintColor
        val sprite: TextureAtlasSprite = FluidSpriteCache.getSprite(handle.stillTexture)

        val matrix = style.poseMatrix()
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

    override fun toString(): String {
        return "FluidBar(fluid:${fluid.description},${super.toString()})"
    }
}
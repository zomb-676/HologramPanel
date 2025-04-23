package com.github.zomb_676.hologrampanel.widget.element.progress

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement.Companion.shortDescription
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions
import net.minecraftforge.fluids.FluidType

class FluidBarElement(progress: ProgressData, val fluid: FluidType) : ProgressBarElement(progress) {
    override fun requireOutlineDecorate(): Boolean = true

    override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
        val left = left.toFloat()
        val right = right.toFloat()
        val height = height.toFloat()

        val handle: IClientFluidTypeExtensions = IClientFluidTypeExtensions.of(fluid)
        val tintColor = handle.tintColor
        @Suppress("DEPRECATION") val sprite: TextureAtlasSprite = Minecraft.getInstance().getModelManager()
            .getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(handle.stillTexture)

        RenderSystem.setShaderTexture(0, sprite.atlasLocation())
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader)
        val matrix = style.poseMatrix()
        val consumer = Tesselator.getInstance().builder
        consumer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)

        val maxU = (((sprite.u1 - sprite.u0) * percent) + sprite.u0).toFloat()
        val maxV = (((sprite.v1 - sprite.v0) * percent) + sprite.v0).toFloat()

        consumer.vertex(matrix, left, 0f, 0f).uv(sprite.u0, sprite.v0).color(tintColor).endVertex()
        consumer.vertex(matrix, left, height, 0f).uv(sprite.u0, maxV).color(tintColor).endVertex()
        consumer.vertex(matrix, right, height, 0f).uv(maxU, maxV).color(tintColor).endVertex()
        consumer.vertex(matrix, right, 0f, 0f).uv(maxU, sprite.v0).color(tintColor).endVertex()
        BufferUploader.drawWithShader(consumer.end())
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
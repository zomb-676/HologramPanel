package com.github.zomb_676.hologrampanel.render.state

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.textures.GpuSampler
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f

data class FloatBlitRenderState(
    val pipeline: RenderPipeline,
    val textureSetup: TextureSetup,
    val pose: Matrix3x2f,
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val u0: Float,
    val u1: Float,
    val v0: Float,
    val v1: Float,
    val color: Int,
    val scissorArea: ScreenRectangle?,
    val bounds: ScreenRectangle? = ScreenAreaUtil.getBounds(x0, y0, x1, y1, pose, scissorArea)
) : GuiElementRenderState {
    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x0, y0).setUv(u0, v0).setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x0, y1).setUv(u0, v1).setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x1, y1).setUv(u1, v1).setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x1, y0).setUv(u1, v0).setColor(color)
    }

    override fun pipeline(): RenderPipeline = pipeline

    override fun textureSetup(): TextureSetup = textureSetup

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle? = bounds

    companion object {
        fun GuiGraphicsExtractor.blitSprite(
            renderPipeline: RenderPipeline,
            sprite: TextureAtlasSprite,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            color: Int = -1
        ) {
            if ((x1 - x0).toInt() != 0 && (y1 - y0).toInt() != 0) {
                this.innerBlit(
                    renderPipeline,
                    sprite.atlasLocation(),
                    x0,
                    x1,
                    y0,
                    y1,
                    sprite.u0,
                    sprite.u1,
                    sprite.v0,
                    sprite.v1,
                    color
                )
            }
        }

        fun GuiGraphicsExtractor.innerBlit(
            renderPipeline: RenderPipeline,
            location: Identifier,
            x0: Float,
            x1: Float,
            y0: Float,
            y1: Float,
            u0: Float,
            u1: Float,
            v0: Float,
            v1: Float,
            color: Int = -1
        ) {
            val texture: AbstractTexture = Minecraft.getInstance().textureManager.getTexture(location)
            this.innerBlit(
                renderPipeline,
                texture.getTextureView(),
                texture.getSampler(),
                x0,
                y0,
                x1,
                y1,
                u0,
                u1,
                v0,
                v1,
                color
            )
        }

        fun GuiGraphicsExtractor.innerBlit(
            pipeline: RenderPipeline,
            textureView: GpuTextureView,
            sampler: GpuSampler,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            u0: Float,
            u1: Float,
            v0: Float,
            v1: Float,
            color: Int
        ) {
            submitGuiElementRenderState(FloatBlitRenderState(
                pipeline,
                TextureSetup.singleTexture(textureView, sampler),
                Matrix3x2f(pose()),
                x0,
                y0,
                x1,
                y1,
                u0,
                u1,
                v0,
                v1,
                color,
                peekScissorStack()
            ))
        }
    }
}
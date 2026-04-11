package com.github.zomb_676.hologrampanel.render.state

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import kotlin.math.max
import kotlin.math.min

data class FloatColoredQuadrilateralRenderState(
    val pipeline: RenderPipeline,
    val textureSetup: TextureSetup,
    val pose: Matrix3x2fc,
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val x3: Float,
    val y3: Float,
    val col: Int,
    val scissorArea: ScreenRectangle?,
    val bounds: ScreenRectangle? = ScreenAreaUtil.getBounds(
        min(min(x0, x1), min(x2, x3)),
        min(min(y0, y1), min(y2, y3)),
        max(max(x0, x1), max(x2, x3)),
        max(max(y0, y1), max(y2, y3)),
        pose, scissorArea)
) : GuiElementRenderState {
    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x0, y0).setColor(col)
        vertexConsumer.addVertexWith2DPose(pose, x1, y1).setColor(col)
        vertexConsumer.addVertexWith2DPose(pose, x2, y2).setColor(col)
        vertexConsumer.addVertexWith2DPose(pose, x3, y3).setColor(col)
    }

    override fun pipeline(): RenderPipeline = pipeline

    override fun textureSetup(): TextureSetup = textureSetup

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle? = bounds

    companion object {
        fun GuiGraphicsExtractor.fillQuad(x0: Float, y0: Float, x1: Float, y1: Float,
                                          x2: Float, y2: Float, x3: Float, y3: Float, col: Int) {
            fillQuad(RenderPipelines.GUI, x0, y0, x1, y1, x2, y2, x3, y3, col)
        }

        fun GuiGraphicsExtractor.fillQuad(pipeline: RenderPipeline, x0: Float, y0: Float, x1: Float, y1: Float,
                                          x2: Float, y2: Float, x3: Float, y3: Float, col: Int) {
            fillQuad(pipeline, TextureSetup.noTexture(), x0, y0, x1, y1, x2, y2, x3, y3, col)
        }

        fun GuiGraphicsExtractor.fillQuad(renderPipeline: RenderPipeline, textureSetup: TextureSetup, x0: Float, y0: Float, x1: Float, y1: Float,
                                          x2: Float, y2: Float, x3: Float, y3: Float) {
            fillQuad(renderPipeline, textureSetup, x0, y0, x1, y1, x2, y2, x3, y3, -1)
        }

        private fun GuiGraphicsExtractor.fillQuad(
            renderPipeline: RenderPipeline,
            textureSetup: TextureSetup,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
            x3: Float,
            y3: Float,
            col: Int
        ) {
            this.submitGuiElementRenderState(FloatColoredQuadrilateralRenderState(
                renderPipeline,
                textureSetup,
                Matrix3x2f(pose()),
                x0,
                y0,
                x1,
                y1,
                x2,
                y2,
                x3,
                y3,
                col,
                peekScissorStack()
            ))
        }
    }
}
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

data class FloatColoredRectangleRenderState(
    val pipeline: RenderPipeline,
    val textureSetup: TextureSetup,
    val pose: Matrix3x2fc,
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val col1: Int,
    val col2: Int,
    val scissorArea: ScreenRectangle?,
    val bounds: ScreenRectangle? = ScreenAreaUtil.getBounds(x0, y0, x1, y1, pose, scissorArea)
) : GuiElementRenderState {
    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x0, y0).setColor(col1)
        vertexConsumer.addVertexWith2DPose(pose, x0, y1).setColor(col2)
        vertexConsumer.addVertexWith2DPose(pose, x1, y1).setColor(col2)
        vertexConsumer.addVertexWith2DPose(pose, x1, y0).setColor(col1)
    }

    override fun pipeline(): RenderPipeline = pipeline

    override fun textureSetup(): TextureSetup = textureSetup

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle? = bounds

    companion object {
        fun GuiGraphicsExtractor.fill(x0: Float, y0: Float, x1: Float, y1: Float, col: Int) {
            fill(RenderPipelines.GUI, x0, y0, x1, y1, col)
        }

        fun GuiGraphicsExtractor.fill(pipeline: RenderPipeline, x0: Float, y0: Float, x1: Float, y1: Float, col: Int) {
            var x0 = x0
            var y0 = y0
            var x1 = x1
            var y1 = y1
            if (x0 < x1) {
                val tmp = x0
                x0 = x1
                x1 = tmp
            }

            if (y0 < y1) {
                val tmp = y0
                y0 = y1
                y1 = tmp
            }

            innerFill(pipeline, TextureSetup.noTexture(), x0, y0, x1, y1, col, null)
        }

        fun GuiGraphicsExtractor.fillGradient(x0: Float, y0: Float, x1: Float, y1: Float, col1: Int, col2: Int) {
            innerFill(RenderPipelines.GUI, TextureSetup.noTexture(), x0, y0, x1, y1, col1, col2)
        }

        fun GuiGraphicsExtractor.fill(renderPipeline: RenderPipeline, textureSetup: TextureSetup, x0: Float, y0: Float, x1: Float, y1: Float) {
            innerFill(renderPipeline, textureSetup, x0, y0, x1, y1, -1, null)
        }

        private fun GuiGraphicsExtractor.innerFill(
            renderPipeline: RenderPipeline,
            textureSetup: TextureSetup,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            color1: Int,
            color2: Int?
        ) {
            this.submitGuiElementRenderState(FloatColoredRectangleRenderState(
                renderPipeline,
                textureSetup,
                Matrix3x2f(pose()),
                x0,
                y0,
                x1,
                y1,
                color1,
                color2 ?: color1,
                peekScissorStack()
            ))
        }
    }
}
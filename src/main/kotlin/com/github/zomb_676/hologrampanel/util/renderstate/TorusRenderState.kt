package com.github.zomb_676.hologrampanel.util.renderstate

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class TorusRenderState(
    val pipeline: RenderPipeline,
    val textureSetup: TextureSetup,
    val pose: Matrix3x2f,
    val inRadius: Float,
    val outRadius: Float,
    val beginRadian: Double,
    val endRadian: Double,
    val tessellationCount: Int,
    val colorIn: Int,
    val colorOut: Int,
    val scissorArea: ScreenRectangle?,
    val bounds: ScreenRectangle? = ScreenAreaUtil.getBounds(-outRadius, -outRadius, outRadius, outRadius, pose, scissorArea)
): GuiElementRenderState {
    override fun buildVertices(vertexConsumer: VertexConsumer) {
        var radius = beginRadian
        val step = Math.toRadians(360.0 / tessellationCount).toFloat()

        fun fillVertex(fillVertexRadius: Double) {
            val lastSin = sin(fillVertexRadius).toFloat()
            val lastCos = cos(fillVertexRadius).toFloat()
            vertexConsumer.addVertexWith2DPose(pose, lastSin * inRadius, lastCos * inRadius).setColor(colorIn)
            vertexConsumer.addVertexWith2DPose(pose, lastSin * outRadius, lastCos * outRadius).setColor(colorOut)
        }

        while (true) {
            if (abs(radius - endRadian) > abs(step)) {
                fillVertex(radius)
                radius += step
            } else {
                fillVertex(endRadian)
                break
            }
        }
    }

    override fun pipeline(): RenderPipeline = pipeline

    override fun textureSetup(): TextureSetup = textureSetup

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle? = bounds
}

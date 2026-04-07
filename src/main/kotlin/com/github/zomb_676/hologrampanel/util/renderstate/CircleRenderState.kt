package com.github.zomb_676.hologrampanel.util.renderstate

import com.github.zomb_676.hologrampanel.util.renderstate.FloatBlitRenderState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class CircleRenderState(
    val pipeline: RenderPipeline,
    val textureSetup: TextureSetup,
    val pose: Matrix3x2f,
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
        vertexConsumer.addVertexWith2DPose(pose, 0f, 0f).setColor(colorIn)

        var radius = beginRadian
        val step = Math.toRadians(360.0 / tessellationCount).toFloat()
        while (true) {
            if (abs(radius - endRadian) > abs(step)) {
                vertexConsumer.addVertexWith2DPose(
                    pose, sin(radius).toFloat() * outRadius, cos(radius).toFloat() * outRadius
                ).setColor(colorOut)
                radius += step
            } else {
                break
            }
        }
        vertexConsumer.addVertexWith2DPose(pose, sin(endRadian).toFloat() * outRadius, cos(endRadian).toFloat() * outRadius)
            .setColor(colorOut)
    }

    override fun pipeline(): RenderPipeline = pipeline

    override fun textureSetup(): TextureSetup = textureSetup

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle? = bounds
}
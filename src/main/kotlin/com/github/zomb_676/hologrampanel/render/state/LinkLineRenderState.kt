package com.github.zomb_676.hologrampanel.render.state

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2f
import org.joml.Vector2d
import org.joml.Vector2f
import kotlin.math.*

/**
 * code calculate different required degree by different edge-case, no interesting
 */
data class LinkLineRenderState(
    val pipeline: RenderPipeline,
    val textureSetup: TextureSetup,
    val pose: Matrix3x2f,
    val begin: Vector2d,
    val end: Vector2d,
    val radius: Double,
    val lineLength: Double,
    val scissorArea: ScreenRectangle?,
    val lineColor: Int = -1,
    val halfLineWidth: Float = 0.8f,
    val segment: Int = 50,
    val bounds: ScreenRectangle? = ScreenAreaUtil.getBounds(begin.x, begin.y, end.x, end.y, pose, scissorArea)
): GuiElementRenderState {
    /**
     * Three segment connection line
     */
    override fun buildVertices(vertexConsumer: VertexConsumer) {
        val beginPoint = Vector2d(begin.x + lineLength, begin.y)
        val endPoint = Vector2d(end.x - lineLength, end.y)

        val theta =
            if ((endPoint.x - beginPoint.x) > 2 * radius) {
                calculateTheta(beginPoint, endPoint, radius)
            } else if (beginPoint.x < endPoint.x) {
                calculateTheta2(beginPoint, endPoint, radius)
            } else {
                calculateTheta3(beginPoint, endPoint, radius)
            }

        if (theta == null) {
            val beginX = begin.x.toFloat()
            val beginY = begin.y.toFloat()
            val endX = end.x.toFloat()
            val endY = end.y.toFloat()
            val offset = Vector2f(endY - beginY, beginX - endX).normalize().mul(halfLineWidth)
            vertexConsumer.addVertexWith2DPose(pose, beginX - offset.x, beginY - offset.y).setColor(lineColor)
            vertexConsumer.addVertexWith2DPose(pose, beginX + offset.x, beginY + offset.y).setColor(lineColor)
            vertexConsumer.addVertexWith2DPose(pose, endX + offset.x, endY + offset.y).setColor(lineColor)
            vertexConsumer.addVertexWith2DPose(pose, endX - offset.x, endY - offset.y).setColor(lineColor)
            return
        }

        val beginCircle: Vector2d
        val endCircle: Vector2d
        if (beginPoint.y < endPoint.y) {
            beginCircle = Vector2d(beginPoint.x, beginPoint.y + radius)
            endCircle = Vector2d(endPoint.x, endPoint.y - radius)
            vertexConsumer.addVertexWith2DPose(pose, begin.x.toFloat(), (begin.y - halfLineWidth).toFloat()).setColor(lineColor)
            vertexConsumer.addVertexWith2DPose(pose, begin.x.toFloat(), (begin.y + halfLineWidth).toFloat()).setColor(lineColor)
        } else {
            beginCircle = Vector2d(beginPoint.x, beginPoint.y - radius)
            endCircle = Vector2d(endPoint.x, endPoint.y + radius)
            vertexConsumer.addVertexWith2DPose(pose, begin.x.toFloat(), (begin.y + halfLineWidth).toFloat()).setColor(lineColor)
            vertexConsumer.addVertexWith2DPose(pose, begin.x.toFloat(), (begin.y - halfLineWidth).toFloat()).setColor(lineColor)
        }

        val sign = if (beginPoint.y < endPoint.y) 1 else -1
        repeat(segment) {
            val the = theta * 2 / segment * it
            val sin = sin(the)
            val cos = cos(the)
            vertexConsumer.addVertexWith2DPose(
                pose, (beginCircle.x + sin * (radius + halfLineWidth)).toFloat(),
                (beginCircle.y - cos * sign * (radius + halfLineWidth)).toFloat(),
            ).setColor(lineColor)
            vertexConsumer.addVertexWith2DPose(
                pose, (beginCircle.x + sin * (radius - halfLineWidth)).toFloat(),
                (beginCircle.y - cos * sign * (radius - halfLineWidth)).toFloat(),
            ).setColor(lineColor)
        }
        for (it in segment downTo 0) {
            val the = theta * 2 / segment * it
            val sin = sin(the)
            val cos = cos(the)
            vertexConsumer.addVertexWith2DPose(
                pose, (endCircle.x - sin * (radius - halfLineWidth)).toFloat(),
                (endCircle.y + cos * sign * (radius - halfLineWidth)).toFloat(),
            ).setColor(lineColor)
            vertexConsumer.addVertexWith2DPose(
                pose, (endCircle.x - sin * (radius + halfLineWidth)).toFloat(),
                (endCircle.y + cos * sign * (radius + halfLineWidth)).toFloat(),
            ).setColor(lineColor)
        }

        if (beginPoint.y < endPoint.y) {
            vertexConsumer.addVertexWith2DPose(pose, end.x.toFloat(), (end.y - halfLineWidth).toFloat()).setColor(lineColor)
            vertexConsumer.addVertexWith2DPose(pose, end.x.toFloat(), (end.y + halfLineWidth).toFloat()).setColor(lineColor)
        } else {
            vertexConsumer.addVertexWith2DPose(pose, end.x.toFloat(), (end.y + halfLineWidth).toFloat()).setColor(lineColor)
            vertexConsumer.addVertexWith2DPose(pose, end.x.toFloat(), (end.y - halfLineWidth).toFloat()).setColor(lineColor)
        }
    }

    override fun pipeline(): RenderPipeline = pipeline

    override fun textureSetup(): TextureSetup = textureSetup

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle? = bounds

    private fun calculateTheta(begin: Vector2d, end: Vector2d, radius: Double): Double? {
        val a = abs(begin.x - end.x)
        val b = abs(begin.y - end.y)
        val denominator = 4 * radius - b

        if (denominator == 0.0 || radius <= 0f) return null

        val delta = a * a - (4 * radius * b - b * b)
        if (delta < 0) return null

        val sqrtDelta = sqrt(delta)
        val t1 = (a + sqrtDelta) / denominator
        val t2 = (a - sqrtDelta) / denominator

        val res = when {
            t1 >= 0 && (a - 2 * radius * t1) > 0 -> atan(t1)
            t2 >= 0 && (a - 2 * radius * t2) > 0 -> atan(t2)
            else -> return null
        }

        require(tan(atan(res)) - res < 1e-3)

        return res
    }

    private fun calculateTheta2(begin: Vector2d, end: Vector2d, radius: Double): Double? {
        val deltaX = abs(end.x - begin.x)
        val deltaY = abs(end.y - begin.y)
        val denominator = 4 * radius - deltaY

        // avoid denominator is zero to divide zero
        if (denominator == 0.0) {
            if (deltaX == 0.0) return null
            val t = (2 * radius) / deltaX
            val originalDenominator = -deltaX + 2 * radius * t
            if (originalDenominator == 0.0) return null
            val theta = atan(t)
            return if (abs(deltaY / originalDenominator - tan(PI - 2 * theta)) < 1e-6) theta else null
        }

        val discriminant = deltaX * deltaX - (4 * radius - deltaY) * deltaY
        if (discriminant < 0) return null

        val sqrtDiscriminant = sqrt(discriminant)
        return atan((deltaX - sqrtDiscriminant) / denominator)
    }

    private fun calculateTheta3(begin: Vector2d, end: Vector2d, radius: Double): Double? {
        val dx = abs(end.x - begin.x)
        val dy = abs(end.y - begin.y)

        val denominator = dy - 4 * radius
        if (denominator <= 0) return null

        val discriminant = dx * dx + dy * dy - 4 * radius * dy
        if (discriminant < 0) return null

        val sqrtTerm = sqrt(discriminant)
        val numerator = dx + sqrtTerm

        val t = numerator / denominator

        return atan(t)
    }
}
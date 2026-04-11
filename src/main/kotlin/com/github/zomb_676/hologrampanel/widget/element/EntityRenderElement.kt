package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.stack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.minus
import kotlin.math.ceil
import kotlin.math.floor

/**
 * render an [entity] not use [LivingEntityRenderElement]
 */
open class EntityRenderElement(val entity: Entity, val entityScale: Double) : RenderElement() {
    companion object {
        val QUATERNION: Quaternionf = Quaternionf().rotateXYZ(Math.toRadians(15.0).toFloat(), 0f, Math.PI.toFloat())
    }

    override fun measureContentSize(style: HologramStyle): Size {
        return Size.Companion.of(
            floor(entity.bbWidth * entityScale * 2).toInt(), floor(entity.bbHeight * entityScale).toInt()
        ).expandWidth(2).expandHeight(2).scale()
    }

    override fun render(style: HologramStyle, partialTicks: Float) {
        val guiGraphics = style.guiGraphics
        style.stack {
            val renderState = extractRenderState(entity)

            val p0 = style.pose().transformPosition(0f, 0f, Vector2f())
            val p2 = style.pose().transformPosition(1f, 1f, Vector2f())
            val componentScale = p2.sub(p0).x
            val actualScale = entityScale.toFloat() * componentScale

            val p1 = style.pose().transformPosition(
                renderState.boundingBoxWidth * entityScale.toFloat() * 2,
                renderState.boundingBoxHeight * entityScale.toFloat() * 2,
                Vector2f()
            )
            val translation = Vector2f(
                p0.x - p0.x.toInt() + p1.x - ceil(p1.x),
                p0.y - p0.y.toInt() + p1.y - ceil(p1.y)
            )
            translation.div(actualScale * 2)
            guiGraphics.entity(renderState, actualScale, Vector3f(translation, 1f), QUATERNION, null,
                p0.x.toInt(), p0.y.toInt(),
                ceil(p1.x).toInt(),
                ceil(p1.y).toInt())
        }
    }

    private fun extractRenderState(entity: Entity): EntityRenderState {
        val entityRenderDispatcher = Minecraft.getInstance().entityRenderDispatcher
        val renderer = entityRenderDispatcher.getRenderer(entity)
        val renderState: EntityRenderState = renderer.createRenderState(entity, 1.0f)
        renderState.shadowPieces.clear()
        renderState.outlineColor = 0
        return renderState
    }

    protected fun renderEntityOutline(style: HologramStyle) {
        val colorWhite = -1
        style.guiGraphics
        val centerX = contentSize.width / 2
        val halfWidth = entity.bbWidth * entityScale * getScale()
        val height = entity.bbHeight * entityScale * getScale()
        style.drawHorizontalLine(-1000, 1000, contentSize.height, colorWhite)
        style.drawHorizontalLine(-1000, 1000, (contentSize.height - height).toInt(), colorWhite)
        style.drawVerticalLine(-1000, +1000, (centerX - halfWidth).toInt(), colorWhite)
        style.drawVerticalLine(-1000, +1000, (centerX + halfWidth).toInt(), colorWhite)
    }

    override fun toString(): String {
        return "EntityRenderElement(entity=${entity.javaClass.simpleName}, entityScale=$entityScale)"
    }
}
package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.stack
import com.mojang.blaze3d.platform.Lighting
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.world.entity.Entity
import org.joml.Quaternionf
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
            style.translate(contentSize.width.toFloat() / 2, contentSize.height.toFloat(), 50.0f)
            val entityScale = entityScale.toFloat()
            style.scale(entityScale, entityScale, -entityScale)
            style.mulPose(QUATERNION)
            guiGraphics.flush()
            Lighting.setupForEntityInInventory()
            val dispatcher = Minecraft.getInstance().entityRenderDispatcher

            dispatcher.setRenderShadow(false)
            dispatcher.render(
                entity, 0.0, 0.0, 0.0, 1.0f, guiGraphics.pose(), guiGraphics.bufferSource, LightTexture.FULL_BRIGHT
            )
            guiGraphics.flush()
            dispatcher.setRenderShadow(true)
            Lighting.setupFor3DItems()
        }
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
package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.floor

/**
 * use [InventoryScreen.renderEntityInInventory] which limits [LivingEntity]
 *
 * use [EntityRenderElement] which resolves that
 */
@Deprecated("use the entity variant", replaceWith = ReplaceWith("EntityRenderElement"), DeprecationLevel.HIDDEN)
open class LivingEntityRenderElement(val entity: LivingEntity, val entityScale: Double) : RenderElement() {
    companion object {
        val QUATERNION: Quaternionf = Quaternionf().rotateZ(Math.PI.toFloat())
        val EMPTY_VECTOR = Vector3f()
    }

    override fun measureContentSize(style: HologramStyle): Size {
        return Size.Companion.of(
            floor(entity.bbWidth * entityScale * 2).toInt(), floor(entity.bbHeight * entityScale).toInt()
        ).expandWidth(2).expandHeight(2).scale()
    }

    override fun render(style: HologramStyle, partialTicks: Float) {
        InventoryScreen.renderEntityInInventory(
            style.guiGraphics,
            contentSize.width.toFloat() / 2,
            contentSize.height.toFloat(),
            entityScale.toFloat(),
            EMPTY_VECTOR,
            QUATERNION,
            null,
            entity
        )
    }

    protected fun renderEntityOutline(style: HologramStyle) {
        val colorWhite = -1
        val graphics = style.guiGraphics
        val centerX = contentSize.width / 2
        val halfWidth = entity.bbWidth * entityScale * getScale()
        val height = entity.bbHeight * entityScale * getScale()
        graphics.hLine(-1000, 1000, contentSize.height, colorWhite)
        graphics.hLine(-1000, 1000, (contentSize.height - height).toInt(), colorWhite)
        graphics.vLine((centerX - halfWidth).toInt(), -1000, +1000, colorWhite)
        graphics.vLine((centerX + halfWidth).toInt(), -1000, +1000, colorWhite)
    }

}
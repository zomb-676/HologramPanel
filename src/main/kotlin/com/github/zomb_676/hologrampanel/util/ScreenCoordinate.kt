package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.joml.Vector2f
import org.joml.Vector4f


/**
 * indicates position in different space
 *
 * @param vector should not be internal changed as design, only use x and y
 */
@JvmInline
value class ScreenCoordinate private constructor(@PublishedApi internal val vector: Vector4f) {
    /**
     * ndc space [-1,1] left-right
     */
    inline val ndcX get() = vector.x

    /**
     * ndc space [-1,1] down-up, opposite to minecraft screen space direction
     */
    inline val ndcY get() = vector.y

    /**
     * ndc space [0,1] left-right
     */
    inline val normalizedX get() = (ndcX / +2.0f) + 0.5f

    /**
     * ndc space [0,1] up-down, opposite to minecraft screen space direction
     */
    inline val normalizedY get() = (ndcY / -2.0f) + 0.5f

    /**
     * minecraft screen-space, consider [com.mojang.blaze3d.platform.Window.guiScale]
     */
    inline val screenX get() = normalizedX * Minecraft.getInstance().window.guiScaledWidth

    /**
     * minecraft screen-space, consider [com.mojang.blaze3d.platform.Window.guiScale]
     */
    inline val screenY get() = normalizedY * Minecraft.getInstance().window.guiScaledHeight

    operator fun component1() = screenX
    operator fun component2() = screenY

    /**
     * translate fraction part and return integer part
     */
    fun equivalentSmooth(poseStack: PoseStack): ScreenPosition {
        val screenX = screenX
        val screenY = screenY
        val x = screenX.toInt()
        val y = screenY.toInt()
        poseStack.translate(screenX - x, screenY - y, 0f)
        return ScreenPosition.of(x, y)
    }

    fun equivalentSmooth(guiGraphics: GuiGraphics) =
        equivalentSmooth(guiGraphics.pose())

    fun equivalentSmooth(hologramStyle: HologramStyle) =
        equivalentSmooth(hologramStyle.guiGraphics)

    /**
     * consider this position is center of the size
     */
    fun inScreen(size: Size): Boolean {
        val window = Minecraft.getInstance().window

        val windowWidth = window.guiScaledWidth
        val x = this.screenX
        if (x > windowWidth / 2) {
            if (x - size.width / 2 > windowWidth) return false
        } else {
            if (x + size.width / 2 < 0) return false
        }

        val windowHeight = window.guiScaledHeight
        val y = this.screenY
        if (y > windowHeight / 2) {
            if (y - size.height / 2 > windowHeight) return false
        } else {
            if (y + size.height / 2 < 0) return false
        }
        return true
    }

    companion object {

        /**
         * @param f take the ownership
         */
        fun of(f: Vector4f): ScreenCoordinate {
            return ScreenCoordinate(f)
        }

        /**
         * @param f will be copied
         */
        fun of(f: Vector2f): ScreenCoordinate {
            val vector = Vector4f(f.x, f.y, 0.0f, 1.0f)
            return ScreenCoordinate(vector)
        }

        val ZERO = of(Vector2f(0f, 0f))
    }
}
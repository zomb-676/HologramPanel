package com.github.zomb_676.hologrampanel.util.packed

import net.minecraft.client.Minecraft
import org.joml.Vector4f


/**
 * indicates position in different space
 *
 * @param vector should not be internally changed as design, only use x and y, under normalized device coordinates (NDC)
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

    inline val screenPosition get() = ScreenPosition.of(screenX, screenY)

    companion object {

        /**
         * @param f take the ownership, the input value should not be changed anymore
         */
        fun of(f: Vector4f): ScreenCoordinate {
            return ScreenCoordinate(f)
        }
    }
}
package com.github.zomb_676.hologrampanel.util.packed

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/**
 * pack two 32-bit float into a 64-bit long
 *
 * by [java.lang.Float.floatToRawIntBits] and [java.lang.Float.floatToIntBits]
 */
@JvmInline
value class ScreenPosition private constructor(@PublishedApi internal val data: Long) {
    inline val x: Float get() = Float.fromBits((data shr 32).toInt())
    inline val y: Float get() = Float.fromBits(data.toInt())

    operator fun component1() = x
    operator fun component2() = y

    override fun toString(): String = "x:$x,y:$y"

    operator fun unaryMinus() = of(-x, -y)

    /**
     * translate fraction part and return integer part
     */
    fun equivalentSmooth(poseStack: PoseStack): AlignedScreenPosition {
        val screenX = x
        val screenY = y
        val x = screenX.toInt()
        val y = screenY.toInt()
        poseStack.translate(screenX - x, screenY - y, 0f)
        return AlignedScreenPosition.of(x, y)
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
        val x = this.x
        if (x > windowWidth / 2) {
            if (x - size.width / 2 > windowWidth) return false
        } else {
            if (x + size.width / 2 < 0) return false
        }

        val windowHeight = window.guiScaledHeight
        val y = this.y
        if (y > windowHeight / 2) {
            if (y - size.height / 2 > windowHeight) return false
        } else {
            if (y + size.height / 2 < 0) return false
        }
        return true
    }

    companion object {
        /**
         * must & 0xFFFFFFFFL to clear high 32 bit, if value is negative, they will be filled with 1, which will influence |
         */
        fun of(x: Float, y: Float): ScreenPosition {
            val intX = java.lang.Float.floatToIntBits(x)
            val intY = java.lang.Float.floatToIntBits(y)
            return ScreenPosition((intX.toLong() shl Int.SIZE_BITS) or (intY.toLong() and 0xFFFFFFFFL))
        }

        val ZERO = of(0f, 0f)
    }
}
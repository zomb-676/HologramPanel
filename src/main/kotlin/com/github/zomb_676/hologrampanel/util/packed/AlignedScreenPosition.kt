package com.github.zomb_676.hologrampanel.util.packed

import net.minecraft.client.Minecraft

/**
 * pack two 32-bit float into a 64-bit long via bit operation
 */
@JvmInline
value class AlignedScreenPosition private constructor(@PublishedApi internal val position: Long) {
    inline val x: Int get() = (position ushr Int.SIZE_BITS).toInt()
    inline val y: Int get() = position.toInt()

    operator fun component1() = x
    operator fun component2() = y

    override fun toString(): String = "x:$x,y:$y"

    /**
     * consider this position is center of the size
     */
    fun inScreen(size: Size): Boolean {
        val window = Minecraft.getInstance().window

        val windowWidth = window.guiScaledWidth
        if (this.x > windowWidth / 2) {
            if (this.x - size.width / 2 > windowWidth) return false
        } else {
            if (this.x + size.width / 2 < 0) return false
        }

        val windowHeight = window.guiScaledHeight
        if (this.y > windowHeight / 2) {
            if (this.y - size.height / 2 > windowHeight) return false
        } else {
            if (this.y + size.height / 2 < 0) return false
        }
        return true
    }

    operator fun unaryMinus() = of(-x, -y)

    fun toNotAligned(): ScreenPosition = ScreenPosition.of(x.toFloat(), y.toFloat())

    companion object {
        /**
         * must & 0xFFFFFFFFL to clear high 32 bit, if value is negative, they will be filled with 1, which will influence |
         */
        fun of(x: Int, y: Int): AlignedScreenPosition = AlignedScreenPosition((x.toLong() shl Int.SIZE_BITS) or (y.toLong() and 0xFFFFFFFFL))

        val ZERO = of(0, 0)
    }
}
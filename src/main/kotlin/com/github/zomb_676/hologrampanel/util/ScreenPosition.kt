package com.github.zomb_676.hologrampanel.util

import net.minecraft.client.Minecraft

@JvmInline
value class ScreenPosition private constructor(@PublishedApi internal val position: Long) {
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

    companion object {
        fun of(x: Int, y: Int) = ScreenPosition((x.toLong() shl Int.SIZE_BITS) or (y.toLong()))
        val ZERO = Size.of(0, 0)
    }
}
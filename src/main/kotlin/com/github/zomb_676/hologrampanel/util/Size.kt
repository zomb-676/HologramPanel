package com.github.zomb_676.hologrampanel.util

import net.minecraft.client.Minecraft

@JvmInline
value class Size private constructor(@PublishedApi internal val size: Long) {
    inline val width: Int get() = (size ushr Int.SIZE_BITS).toInt()
    inline val height: Int get() = size.toInt()

    operator fun component1() = width
    operator fun component2() = height

    fun expandWidth(value: Int) = of(width + value, height)
    fun expandHeight(value: Int) = of(width, height + value)
    fun shrinkWidth(value: Int) = of(width - value, height)
    fun shrinkHeight(value: Int) = of(width, height - value)
    fun expand(padding: Padding) = of(width + padding.horizontal, height + padding.vertical)

    override fun toString(): String = "width:$width,height:$height"

    companion object {
        fun of(width: Int, height: Int) = Size((width.toLong() shl Int.SIZE_BITS) or (height.toLong()))

        fun of(length : Int) = of(length, length)

        fun ofMinecraftWindowScaledSize(): Size {
            val window = Minecraft.getInstance().window
            return of(window.guiScaledWidth, window.guiScaledHeight)
        }

        val ZERO = of(0, 0)
    }
}
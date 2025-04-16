package com.github.zomb_676.hologrampanel.util.rect

import org.lwjgl.stb.STBRPRect

/**
 * a wrapper class for [STBRPRect]
 */
@JvmInline
value class PackedRect(@PublishedApi internal val data: STBRPRect.Buffer) {
    inline val x get() = data.x()
    inline val y get() = data.y()
    inline val width get() = data.w()
    inline val height get() = data.h()

    inline val assigned get() = data.was_packed()

    companion object {
        val EMPTY = PackedRect(STBRPRect.create(1))
    }

    override fun toString(): String {
        return "Rect(x=$x, y=$y, w=$width, h=$height, s=$assigned)"
    }
}


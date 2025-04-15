package com.github.zomb_676.hologrampanel.util.rect

import org.lwjgl.stb.STBRPContext
import org.lwjgl.stb.STBRPNode
import org.lwjgl.stb.STBRPRect
import org.lwjgl.stb.STBRectPack

class RectAllocator(val width: Int, val height: Int)  {
    private val temporaryArray = STBRPNode.create(width)
    val context = STBRPContext.create().also { context ->
        STBRectPack.stbrp_init_target(context, width, height, temporaryArray)
    }

    fun allocate(width: Int, height: Int): PackedRect {
        val data = STBRPRect.create(1)
        data.get(0).w(width).h(height)
        STBRectPack.stbrp_pack_rects(context, data)
        return PackedRect(data)
    }

    fun fresh() {
        STBRectPack.stbrp_init_target(context, width, height, temporaryArray)
    }
}
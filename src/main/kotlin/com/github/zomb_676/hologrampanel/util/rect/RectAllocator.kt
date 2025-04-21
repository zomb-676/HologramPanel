package com.github.zomb_676.hologrampanel.util.rect

import org.lwjgl.stb.STBRPContext
import org.lwjgl.stb.STBRPNode
import org.lwjgl.stb.STBRPRect
import org.lwjgl.stb.STBRectPack

/**
 * warp [STBRectPack] which use the skyline arithmetic, here we will continue
 * to allocate and [fresh] it
 */
class RectAllocator(private var width: Int, private var height: Int) {
    private val temporaryArray = STBRPNode.create(width)
    private val context = STBRPContext.create().also { context ->
        STBRectPack.stbrp_init_target(context, width, height, temporaryArray)
    }

    /**
     * should manual check [PackedRect.assigned]
     */
    fun allocate(width: Int, height: Int): PackedRect {
        val data = STBRPRect.create(1)
        data.get(0).w(width).h(height)
        STBRectPack.stbrp_pack_rects(context, data)
        return PackedRect(data)
    }

    fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun fresh() {
        STBRectPack.stbrp_init_target(context, width, height, temporaryArray)
    }

    override fun toString(): String {
        return "RectAllocator(width=$width, height=$height)"
    }
}
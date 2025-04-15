package com.github.zomb_676.hologrampanel.render

import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.rect.PackedRect
import com.github.zomb_676.hologrampanel.util.rect.RectAllocator
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL46
import kotlin.math.max
import kotlin.math.min

object TransitRenderTargetManager {
    const val MIN_WIDTH = 1920
    const val MIN_HEIGHT = 1080

    val MAX_HEIGHT = GL46.glGetInteger(GL46.GL_MAX_FRAMEBUFFER_WIDTH)
    val MAX_WIDTH = GL46.glGetInteger(GL46.GL_MAX_FRAMEBUFFER_HEIGHT)

    private val entries: MutableList<TransitRenderTarget> = mutableListOf()

    private class TransitRenderTarget(width: Int, height: Int) : TextureTarget(width, height, true, false) {
        val allocator = RectAllocator(width, height)

        fun refreshAllocator() {
            allocator.fresh()
        }

        fun tryAllocate(width: Int, height: Int): PackedRect = allocator.allocate(width, height)
    }

    private fun create(): TransitRenderTarget {
        val window = Minecraft.getInstance().window
        val width = min(max(window.width * 2, MIN_WIDTH), MAX_WIDTH)
        val height = min(max(window.height * 2, MIN_HEIGHT), MAX_HEIGHT)
        return TransitRenderTarget(width, height)
    }

    fun onResize(width: Int, height: Int) {
        val width = min(max(width * 2, MIN_WIDTH), MAX_WIDTH)
        val height = min(max(height * 2, MIN_HEIGHT), MAX_HEIGHT)
        entries.forEach { target ->
            target.resize(width, height)
        }
    }

    fun allocate(width: Int, height: Int, locate: LocateType.World.FacingVector): RenderTarget {
        for (target in entries) {
            val res = target.tryAllocate(width, height)
            if (res.assigned) {
                locate.allocatedSpace = res
                locate.target = target
                return target
            }
        }
        val new = create()
        val res = new.tryAllocate(width, height)
        if (!res.assigned) throw RuntimeException("too big requested width:$width, height:$height")
        locate.allocatedSpace = res
        locate.target = new
        return new
    }

    fun allocate(size: Size, locate: LocateType.World.FacingVector): RenderTarget =
        allocate(size.width, size.height, locate)

    fun refresh() {
        this.entries.forEach(TransitRenderTarget::refreshAllocator)
    }

    fun blitToMain() {

    }
}
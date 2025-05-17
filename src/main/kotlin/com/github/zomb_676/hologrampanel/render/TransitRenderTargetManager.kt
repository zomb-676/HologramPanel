package com.github.zomb_676.hologrampanel.render

import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.rect.PackedRect
import com.github.zomb_676.hologrampanel.util.rect.RectAllocator
import com.github.zomb_676.hologrampanel.widget.locateType.LocateFreelyInWorld
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.CoreShaders
import org.lwjgl.opengl.GL46
import java.util.*

/**
 * render all [LocateFreelyInWorld] at [TransitRenderTarget] then, blit
 */
object TransitRenderTargetManager {

    private val entries: SequencedMap<TransitRenderTarget, MutableList<HologramRenderState>> = Object2ObjectLinkedOpenHashMap()
    private val interactRenderTarget: TransitRenderTarget = TransitRenderTarget.create()

    /**
     * if we use a FrameBuffer as a temporary target for minecraft screen space draw,
     * its size should be recommended the same as [com.mojang.blaze3d.pipeline.MainTarget],
     * so that we can use the same `ORTHOGRAPHIC` [RenderSystem.getProjectionMatrix]
     *
     * otherwise, you should set that together with [GL46.glViewport]
     *
     */
    private class TransitRenderTarget private constructor(width: Int, height: Int) : TextureTarget(width, height, true, false) {
        /**
         * the allocator should not use size measured by pixel.
         * The [com.mojang.blaze3d.platform.Window.guiScale] should be taken into consideration
         *
         * we can easily use the scaled size as we keep the same size as [com.mojang.blaze3d.pipeline.MainTarget].
         * additionally, allocated size should be changed when gui scale changed
         */
        val allocator: RectAllocator

        init {
            val window = Minecraft.getInstance().window
            allocator = RectAllocator(window.guiScaledWidth, window.guiScaledHeight)
        }

        /**
         * clear all the allocated records for the next allocate cycle
         */
        fun refreshAllocator() {
            allocator.fresh()
        }

        fun tryAllocate(width: Int, height: Int): PackedRect = allocator.allocate(width, height)

        companion object {
            fun create(): TransitRenderTarget {
                val mainTarget = Minecraft.getInstance().mainRenderTarget
                val target = TransitRenderTarget(mainTarget.width, mainTarget.height)
                target.setClearColor(1f, 1f, 1f, 0f)
                return target
            }
        }
    }

    fun onResize(width: Int, height: Int) {
        entries.keys.forEach { target ->
            target.resize(width, height)
        }
        interactRenderTarget.resize(width, height)
        onGuiScaleChange()
    }

    /**
     * allocate [width] by [height] and record its result into [locate]
     */
    fun allocate(width: Int, height: Int, locate: LocateFreelyInWorld, state: HologramRenderState): RenderTarget {
        for ((target, records) in entries) {
            val res = target.tryAllocate(width, height)
            if (res.assigned) {
                locate.setAllocatedSpace(res)
                locate.target = target
                records.add(state)
                return target
            }
        }
        val new = TransitRenderTarget.create()
        Minecraft.getInstance().mainRenderTarget.bindWrite(true)
        val res = new.tryAllocate(width, height)
        if (!res.assigned) throw RuntimeException("too big requested width:$width, height:$height")
        entries.put(new, mutableListOf(state))
        locate.setAllocatedSpace(res)
        locate.target = new
        return new
    }

    fun allocate(size: Size, locate: LocateFreelyInWorld, state: HologramRenderState): RenderTarget =
        allocate(size.width, size.height, locate, state)

    fun refresh() {
        this.entries.forEach { (target, records) ->
            target.refreshAllocator()
            records.clear()
        }
    }

    /**
     * blit all the internal [TransitRenderTarget] to [com.mojang.blaze3d.pipeline.MainTarget] for debug usage
     */
    fun blitAllTransientTargetToMain(style: HologramStyle) {
        val mainTarget = Minecraft.getInstance().mainRenderTarget
        mainTarget.bindWrite(true)
        RenderSystem.colorMask(true, true, true, false)
        RenderSystem.disableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.depthMask(false)
        RenderSystem.setShader(CoreShaders.POSITION_TEX)
        val allocator = run {
            val window = Minecraft.getInstance().window
            RectAllocator(window.guiScaledWidth, window.guiScaledHeight)
        }
        val sequence = sequence {
            yield(interactRenderTarget)
            yieldAll(entries.keys)
        }
        for (target in sequence) {
            val rect = allocator.allocate(target.width / 16, target.height / 16)
            if (!rect.assigned) break
            RenderSystem.setShaderTexture(0, target.getColorTextureId())
            val builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
            val x = rect.x.toFloat()
            val y = rect.y.toFloat()
            val w = rect.width.toFloat()
            val h = rect.height.toFloat()
            builder.addVertex(x, y, 0f).setUv(0f, 1f)
            builder.addVertex(x, y + h, 0f).setUv(0f, 0f)
            builder.addVertex(x + w, y + h, 0f).setUv(1f, 0f)
            builder.addVertex(x + w, y, 0f).setUv(1f, 1f)
            BufferUploader.drawWithShader(builder.buildOrThrow())

            style.guiGraphics.renderOutline(rect.x, rect.y, rect.width, rect.height, -1)
        }
        style.guiGraphics.flush()

        RenderSystem.depthMask(true)
        RenderSystem.colorMask(true, true, true, true)
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, mainTarget.frameBufferId)
    }

    /**
     * used to iterator all the allocated contents
     */
    fun getEntries(): Iterator<Map.Entry<RenderTarget, MutableList<HologramRenderState>>> = entries.iterator()

    fun getInteractTarget(): RenderTarget = interactRenderTarget

    fun onGuiScaleChange() {
        val window = Minecraft.getInstance().window
        val width = window.guiScaledWidth
        val height = window.guiScaledHeight
        this.entries.forEach { (entry, _) ->
            entry.allocator.resize(width, height)
        }
        interactRenderTarget.allocator.resize(width, height)
    }
}
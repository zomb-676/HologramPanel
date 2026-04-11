package com.github.zomb_676.hologrampanel.render

import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.util.PipRendererRecorder
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.rect.PackedRect
import com.github.zomb_676.hologrampanel.util.rect.RectAllocator
import com.github.zomb_676.hologrampanel.widget.locateType.LocateFreelyInWorld
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.renderer.state.gui.GuiRenderState
import java.util.*

/**
 * render all [LocateFreelyInWorld] at [TransitRenderTarget] then, blit
 */
object TransitRenderTargetManager {

    private val entries: SequencedMap<TransitRenderTarget, MutableList<HologramRenderState>> = Object2ObjectLinkedOpenHashMap()
    private val interactRenderTarget: TransitRenderTarget = TransitRenderTarget.create()
    val customGuiRenderState = GuiRenderState()
    val customGuiRenderer = GuiRenderer(
        customGuiRenderState,
        Minecraft.getInstance().renderBuffers().bufferSource(),
        Minecraft.getInstance().gameRenderer.submitNodeStorage,
        Minecraft.getInstance().gameRenderer.featureRenderDispatcher,
        PipRendererRecorder.pipRenderers
    )

    /**
     * if we use a FrameBuffer as a temporary target for minecraft screen space draw,
     * its size should be recommended the same as [com.mojang.blaze3d.pipeline.MainTarget],
     * so that we can use the same `ORTHOGRAPHIC` [RenderSystem.getProjectionMatrix]
     *
     * otherwise, you should set that together with [GL46.glViewport]
     *
     */
    private class TransitRenderTarget private constructor(width: Int, height: Int) : TextureTarget("Transit Render", width, height, true, false) {
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
                return target
            }
        }
    }

    fun RenderTarget.clear() {
        RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(colorTexture!!, 0x00FFFFFF, depthTexture!!, 1.0)
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
            style.guiGraphics.blit(
                target.colorTextureView!!,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),
                rect.x,
                rect.y,
                rect.x + rect.width,
                rect.y + rect.height,
                0f,
                1f,
                1f,
                0f
            )
            style.guiGraphics.outline(rect.x, rect.y, rect.width, rect.height, -1)
        }
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
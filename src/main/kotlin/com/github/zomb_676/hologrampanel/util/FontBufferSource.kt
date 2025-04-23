package com.github.zomb_676.hologrampanel.util

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType

/**
 * used for accelerating batch font render in level
 *
 * assume satisfying [RenderType.canConsolidateConsecutiveGeometry]
 *
 * works under vanilla and ModernUI
 */
class FontBufferSource : MultiBufferSource {
    val buffers: MutableMap<RenderType, BufferBuilder> = mutableMapOf()
    val building: MutableMap<RenderType, BufferBuilder> = Object2ObjectLinkedOpenHashMap()
    override fun getBuffer(renderType: RenderType): VertexConsumer {
        return building.computeIfAbsent(renderType) {
            val buffer = buffers.computeIfAbsent(renderType) {
                BufferBuilder(10240)
            }
            buffer.begin(renderType.mode(), renderType.format())
            buffer
        }
    }

    fun endFontBatch() {
        building.forEach { (type, builder) ->
            type.end(builder, RenderSystem.getVertexSorting())
        }
        building.clear()
    }
}
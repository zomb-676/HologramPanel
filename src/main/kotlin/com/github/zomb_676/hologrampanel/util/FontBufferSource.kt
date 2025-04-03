package com.github.zomb_676.hologrampanel.util

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import java.util.SequencedMap

/**
 * used for accelerating batch font render in level
 *
 * assume satisfying [RenderType.canConsolidateConsecutiveGeometry]
 *
 * works under vanilla and ModernUI
 */
class FontBufferSource : MultiBufferSource {
    val buffers: MutableMap<RenderType, ByteBufferBuilder> = mutableMapOf()
    val building: SequencedMap<RenderType, BufferBuilder> = Object2ObjectLinkedOpenHashMap()
    override fun getBuffer(renderType: RenderType): VertexConsumer {
        return building.computeIfAbsent(renderType) {
            val buffer = buffers.computeIfAbsent(renderType) {
                ByteBufferBuilder(10240)
            }
            BufferBuilder(buffer, renderType.mode, renderType.format)
        }
    }

    fun endFontBatch() {
        building.forEach { (type, builder) ->
            val meshData = builder.build()
            if (meshData != null) {
                if (type.sortOnUpload()) {
                    val buffer = buffers[type]!!
                    meshData.sortQuads(buffer, RenderSystem.getVertexSorting())
                }
                type.draw(meshData)
            }
        }
        building.clear()
    }
}
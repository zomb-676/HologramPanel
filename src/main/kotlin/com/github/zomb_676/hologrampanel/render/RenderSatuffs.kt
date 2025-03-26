package com.github.zomb_676.hologrampanel.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType

/**
 * all [RenderType] and [RenderPipeline]
 */
object RenderStuff {
    object Pipeline {
        val DEBUG_FILLED_BOX_DISABLE_DEPTH: RenderPipeline = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation("pipeline/debug_filled_box")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    }


    object Type {
        val DEBUG_FILLED_BOX_DISABLE_DEPTH : RenderType  = RenderType.create(
            "debug_filled_box",
            1536,
            false,
            true,
            Pipeline.DEBUG_FILLED_BOX_DISABLE_DEPTH,
            RenderType.CompositeState.builder().setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false)
        )
    }
}
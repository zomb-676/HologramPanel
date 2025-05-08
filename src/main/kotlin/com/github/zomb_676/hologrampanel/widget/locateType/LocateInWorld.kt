package com.github.zomb_676.hologrampanel.widget.locateType

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import org.joml.Vector3f

/**
 * indicate the hologram is located by its world space position
 */
sealed interface LocateInWorld : LocateType {
    val offset: Vector3f

    override fun getScreenSpacePosition(context: HologramContext, partialTick: Float) =
        getSourceScreenSpacePosition(context, partialTick)
}
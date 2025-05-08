package com.github.zomb_676.hologrampanel.widget.locateType

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * indicate the hologram is located by its world space position
 */
sealed interface LocateInWorld : LocateType {
    val offset: Vector3f

    override fun getScreenSpacePosition(context: HologramContext, partialTick: Float) =
        getSourceScreenSpacePosition(context, partialTick)

    override fun getSourceWorldPosition(context: HologramContext, partialTick: Float): Vector3fc {
        return super.getSourceWorldPosition(context, partialTick).add(offset, Vector3f())
    }
}
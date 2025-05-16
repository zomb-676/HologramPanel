package com.github.zomb_676.hologrampanel.widget.locateType

import org.joml.Vector3f

/**
 * indicate the hologram is located by its world space position
 */
sealed interface LocateInWorld : LocateType {
    val offset: Vector3f

    override fun applyModifyToWorldPosition(input: Vector3f): Vector3f = input.add(offset)
}
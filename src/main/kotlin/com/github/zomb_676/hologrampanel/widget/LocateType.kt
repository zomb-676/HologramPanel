package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.github.zomb_676.hologrampanel.util.rect.PackedRect
import com.mojang.blaze3d.pipeline.RenderTarget
import org.joml.Vector2f
import org.joml.Vector3f

sealed interface LocateType {

    fun getScreenSpacePosition(hologramContext: HologramContext, partialTick: Float): ScreenPosition

    /**
     * transform [HologramContext.hologramCenterPosition] into minecraft screen space
     */
    fun getSourceScreenSpacePosition(hologramContext: HologramContext, partialTick: Float): ScreenPosition =
        MVPMatrixRecorder.transform(hologramContext.hologramCenterPosition(partialTick)).screenPosition

    sealed interface World : LocateType {
        override fun getScreenSpacePosition(hologramContext: HologramContext, partialTick: Float) =
            getSourceScreenSpacePosition(hologramContext, partialTick)

        data object FacingPlayer : World

        class FacingVector(val direction: Vector3f) : World {
            /**
             * used for operating remapping
             */
            var allocatedSpace: PackedRect = PackedRect.EMPTY

            var target : RenderTarget? = null
        }
    }

    class Screen(val position: Vector2f) : LocateType {
        operator fun component1() = position.x
        operator fun component2() = position.y

        fun setPosition(x: Float, y: Float) {
            position.set(x, y)
        }

        override fun getScreenSpacePosition(hologramContext: HologramContext, partialTick: Float): ScreenPosition =
            ScreenPosition.of(position.x, position.y)

    }
}
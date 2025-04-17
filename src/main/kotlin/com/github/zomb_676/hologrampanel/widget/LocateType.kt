package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.github.zomb_676.hologrampanel.util.rect.PackedRect
import com.mojang.blaze3d.pipeline.RenderTarget
import net.minecraft.client.Camera
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3fc

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

        class FacingVector() : World {
            private val view = Vector3f()
            private val left = Vector3f()
            private val up = Vector3f()
            fun getLeft() : Vector3fc = left
            fun getUp() : Vector3fc = up

            fun byCamera(camera: Camera): FacingVector {
                camera.lookVector.mul(-1f, view)
                view.normalize()
                left.set(camera.leftVector).normalize()
                up.set(camera.upVector).normalize()
                //calculate scale here
                return this
            }

            /**
             * used for operating remapping
             */
            var allocatedSpace: PackedRect = PackedRect.EMPTY

            var target: RenderTarget? = null
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
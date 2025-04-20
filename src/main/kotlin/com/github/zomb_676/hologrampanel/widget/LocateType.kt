package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.github.zomb_676.hologrampanel.util.rect.PackedRect
import com.mojang.blaze3d.pipeline.RenderTarget
import net.minecraft.client.Camera
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3f
import org.joml.Vector3fc

sealed interface LocateType {

    fun getScreenSpacePosition(context: HologramContext, partialTick: Float): ScreenPosition

    /**
     * transform [HologramContext.hologramCenterPosition] into minecraft screen space
     */
    fun getSourceScreenSpacePosition(context: HologramContext, partialTick: Float): ScreenPosition =
        MVPMatrixRecorder.transform(getSourceWorldPosition(context, partialTick)).screenPosition

    fun getSourceWorldPosition(context: HologramContext, partialTick: Float): Vector3fc =
        context.hologramCenterPosition(partialTick)

    sealed interface World : LocateType {

        override fun getScreenSpacePosition(context: HologramContext, partialTick: Float) =
            getSourceScreenSpacePosition(context, partialTick)

        data object FacingPlayer : World

        class FacingVector() : World {
            private val view = Vector3f()
            private val left = Vector3f()
            private val up = Vector3f()
            fun getLeft(): Vector3fc = left
            fun getUp(): Vector3fc = up

            private val leftUp: Vector2f = Vector2f()
            private val leftDown: Vector2f = Vector2f()
            private val rightUp: Vector2f = Vector2f()
            private val rightDown: Vector2f = Vector2f()

            fun getLeftUp(): Vector2fc = leftUp
            fun getLeftDown(): Vector2fc = leftDown
            fun getRightUp(): Vector2fc = rightUp
            fun getRightDown(): Vector2fc = rightDown

            fun updateLeftUp(vector3fc: Vector3fc) {
                MVPMatrixRecorder.transform(vector3fc).screenPosition.set(leftUp)
            }

            fun updateLeftDown(vector3fc: Vector3fc) {
                MVPMatrixRecorder.transform(vector3fc).screenPosition.set(leftDown)
            }

            fun updateRightUp(vector3fc: Vector3fc) {
                MVPMatrixRecorder.transform(vector3fc).screenPosition.set(rightUp)
            }

            fun updateRightDown(vector3fc: Vector3fc) {
                MVPMatrixRecorder.transform(vector3fc).screenPosition.set(rightDown)
            }

            fun isMouseIn(mouseX: Float, mouseY: Float): Boolean {
                fun crossProductZ(p1: Vector2f, p2: Vector2f, checkX: Float, checkY: Float): Float {
                    return (p2.x - p1.x) * (checkY - p1.y) - (p2.y - p1.y) * (checkX - p1.x)
                }

                val cp1 = crossProductZ(leftUp, leftDown, mouseX, mouseY)
                val cp2 = crossProductZ(leftDown, rightDown, mouseX, mouseY)
                val cp3 = crossProductZ(rightDown, rightUp, mouseX, mouseY)
                val cp4 = crossProductZ(rightUp, leftUp, mouseX, mouseY)

                val epsilon = 1e-6
                val allPositive = cp1 > epsilon && cp2 > epsilon && cp3 > epsilon && cp4 > epsilon
                val allNegative = cp1 < -epsilon && cp2 < -epsilon && cp3 < -epsilon && cp4 < -epsilon
                return allPositive || allNegative
            }

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

        override fun getScreenSpacePosition(context: HologramContext, partialTick: Float): ScreenPosition =
            ScreenPosition.of(position.x, position.y)

    }
}
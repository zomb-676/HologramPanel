package com.github.zomb_676.hologrampanel.util

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f

/**
 * transform world-space position into minecraft screen-space location
 *
 * all input positions should be located in minecraft world space anchored by world zero not camera
 */
object MVPMatrixRecorder {

    private val transformMatrix: Matrix4f = Matrix4f()

    fun recordMVPMatrixByCurrentState() {
        this.transformMatrix.set(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix())
    }

    fun transform(vec3: Vec3) = transform(vec3.x, vec3.y, vec3.z)
    fun transform(vec3i: Vec3i) = transform(vec3i.x.toDouble(), vec3i.y.toDouble(), vec3i.z.toDouble())
    fun transformBlockToOffset(vec3i: Vec3i) =
        transform(vec3i.x + 0.5, vec3i.y + 0.5, vec3i.z + 0.5)

    fun transform(x: Float, y: Float, z: Float) = transform(x.toDouble(), y.toDouble(), z.toDouble())

    fun transform(f: Vector3fc) =
        transform(f.x(), f.y(), f.z())

    fun transform(f: Vector4f) =
        transform(f.x, f.y, f.z)

    fun transform(x: Double, y: Double, z: Double): ScreenCoordinate {
        val cameraPosition = Minecraft.getInstance().gameRenderer.mainCamera.position
        val vector = Vector4f(
            (x - cameraPosition.x).toFloat(),
            (y - cameraPosition.y).toFloat(),
            (z - cameraPosition.z).toFloat(),
            1.0f//it is a position vector, so supply 1.0 here
        )
        vector.mul(transformMatrix)
        //do perspective divide
        //in NDC space now
        vector.x /= vector.w
        vector.y /= vector.w

        return ScreenCoordinate.of(vector)
    }
}
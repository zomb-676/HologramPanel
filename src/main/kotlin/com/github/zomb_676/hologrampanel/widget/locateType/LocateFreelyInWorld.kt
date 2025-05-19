package com.github.zomb_676.hologrampanel.widget.locateType

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.util.JomlMath
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.rect.PackedRect
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Camera
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Quaternionfc
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3f
import org.joml.Vector3fc
import java.lang.Math

class LocateFreelyInWorld() : LocateInWorld {
    private val view = Vector3f()
    private val left = Vector3f()
    private val up = Vector3f()

    private val quaternion = Quaternionf()

    var xRot: Float = 0f
        private set
    var yRot: Float = 0f
        private set
    var roll: Float = 0f
        private set

    fun getView(): Vector3fc = view
    fun getLeft(): Vector3fc = left
    fun getUp(): Vector3fc = up

    var scale: Float = 1f
    inline val renderScale get() = 80f / scale

    override val offset: Vector3f = Vector3f()

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

    fun byCamera(camera: Camera): LocateFreelyInWorld {
        this.xRot = camera.xRot
        this.yRot = -camera.yRot
        this.roll = camera.roll
        this.updateRotation()
        this.updateVectors()
        //calculate scale here
        return this
    }

    private fun updateRotation() {
        this.quaternion.identity().rotateYXZ(
            Math.PI.toFloat() - JomlMath.toRadians(-yRot),
            JomlMath.toRadians(-xRot),
            JomlMath.toRadians(-roll)
        ).also {
            if (it.w < 0) {
                it.set(-it.x, -it.y, -it.z, -it.w)
            }
        }
    }

    /**
     * [Camera.setRotation]
     */
    fun updateVectors(): LocateFreelyInWorld {
        FORWARDS.rotate(this.quaternion, this.view)
        UP.rotate(this.quaternion, this.up)
        LEFT.rotate(this.quaternion, this.left)
        return this
    }

    /**
     * follow order and degree remapping in [getRotationQuaternion]
     */
    fun updateEulerDegrees() {
        Matrix4f().rotation(this.quaternion).getEulerAnglesYXZ(Vector3f()).also { eulerAngles ->
            this.xRot = normalizeAngle(JomlMath.toDegrees(-eulerAngles.x))
            this.yRot = normalizeAngle(JomlMath.toDegrees((Math.PI + eulerAngles.y).toFloat()))
            this.roll = normalizeAngle(JomlMath.toDegrees(-eulerAngles.z))
        }
    }

    private fun normalizeAngle(degrees: Float): Float {
        var degrees = degrees
        degrees %= 360f
        if (degrees > 180) {
            degrees -= 360f
        } else if (degrees < -180) {
            degrees += 360f
        }
        return degrees
    }

    /**
     * used for operating remapping
     */
    @OnlyIn(Dist.CLIENT)
    private var allocatedSpace: PackedRect? = null

    @OnlyIn(Dist.CLIENT)
    fun getAllocatedSpace() : PackedRect = allocatedSpace ?: PackedRect.EMPTY

    @OnlyIn(Dist.CLIENT)
    fun setAllocatedSpace(allocatedSpace: PackedRect) {
        this.allocatedSpace = allocatedSpace
    }

    @get:OnlyIn(Dist.CLIENT)
    @set:OnlyIn(Dist.CLIENT)
    var target: RenderTarget? = null

    override fun getLocateEnum(): LocateEnum = LocateEnum.FREELY_IN_WORLD

    fun getRotation(): Quaternionfc = this.quaternion
    internal fun getMutableRotation(): Quaternionf = this.quaternion
    fun setRotation(rotation: Quaternionfc) {
        this.quaternion.set(rotation)
        this.updateVectors()
        this.updateEulerDegrees()
    }

    override fun toString(): String =
        "LocateFreelyInWorld(xRot=$xRot, yRot=$yRot, roll=$roll, scale=$scale, offset=${offset.toString(HologramPanel.NUMBER_FORMAT)})"

    companion object {
        private val FORWARDS: Vector3f = Vector3f(0.0f, 0.0f, -1.0f)
        private val UP: Vector3f = Vector3f(0.0f, 1.0f, 0.0f)
        private val LEFT: Vector3f = Vector3f(-1.0f, 0.0f, 0.0f)

        val CODEC: MapCodec<LocateFreelyInWorld> = RecordCodecBuilder.mapCodec { ins ->
            ins.group(
                Codec.FLOAT.fieldOf("xRot").forGetter(LocateFreelyInWorld::xRot),
                Codec.FLOAT.fieldOf("yRot").forGetter(LocateFreelyInWorld::yRot),
                Codec.FLOAT.fieldOf("roll").forGetter(LocateFreelyInWorld::roll),
                Codec.FLOAT.fieldOf("scale").forGetter(LocateFreelyInWorld::scale),
                AllRegisters.Codecs.VEC3F.fieldOf("offset").forGetter(LocateFreelyInWorld::offset),
            ).apply(ins) { xRot, yRot, roll, scale, offset ->
                LocateFreelyInWorld().also { locate ->
                    locate.xRot = xRot
                    locate.yRot = yRot
                    locate.roll = roll
                    locate.updateRotation()
                    locate.updateVectors()
                    locate.scale = scale
                    locate.offset.set(offset)
                }
            }
        }
    }
}
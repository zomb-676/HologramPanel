package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.JomlMath
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.github.zomb_676.hologrampanel.util.rect.PackedRect
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Camera
import org.joml.*

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

        data object FacingPlayer : World {
            val CODEC: Codec<FacingPlayer> = Codec.unit(FacingPlayer)
        }

        class FacingVector() : World {
            private val view = Vector3f()
            private val left = Vector3f()
            private val up = Vector3f()

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
            val offset: Vector3f = Vector3f()

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
                this.xRot = camera.xRot
                this.yRot = -camera.yRot
                this.roll = camera.roll
                this.updateVectors(getRotationQuaternion())
                //calculate scale here
                return this
            }

            fun setRotation(xRot: Float = this.xRot, yRot: Float = this.yRot, roll: Float = this.roll): FacingVector {
                this.xRot = xRot
                this.yRot = yRot
                this.roll = roll
                this.updateVectors(getRotationQuaternion())
                return this
            }

            /**
             * [Camera.setRotation]
             */
            fun updateVectors(rotation: Quaternionf): FacingVector {
                FORWARDS.rotate(rotation, this.view)
                UP.rotate(rotation, this.up)
                LEFT.rotate(rotation, this.left)
                return this
            }

            /**
             * follow order and degree remapping in [getRotationQuaternion]
             */
            fun updateEulerDegrees(rotation: Quaternionf) {
                Matrix4f().rotation(rotation).getEulerAnglesYXZ(Vector3f()).also { eulerAngles ->
                    this.xRot = normalizeAngle(Math.toDegrees(-eulerAngles.x))
                    this.yRot = normalizeAngle(Math.toDegrees((Math.PI + eulerAngles.y).toFloat()))
                    this.roll = normalizeAngle(Math.toDegrees(-eulerAngles.z))
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

            fun getRotationQuaternion(): Quaternionf = Quaternionf().rotateYXZ(
                Math.PI.toFloat() - JomlMath.toRadians(-yRot),
                JomlMath.toRadians(-xRot),
                JomlMath.toRadians(-roll)
            ).also {
                if (it.w < 0) {
                    it.set(-it.x, -it.y, -it.z, -it.w)
                }
            }

            /**
             * used for operating remapping
             */
            var allocatedSpace: PackedRect = PackedRect.EMPTY

            var target: RenderTarget? = null

            override fun getSourceWorldPosition(context: HologramContext, partialTick: Float): Vector3fc =
                super.getSourceWorldPosition(context, partialTick).add(offset, Vector3f())

            companion object {
                private val FORWARDS: Vector3f = Vector3f(0.0f, 0.0f, -1.0f)
                private val UP: Vector3f = Vector3f(0.0f, 1.0f, 0.0f)
                private val LEFT: Vector3f = Vector3f(-1.0f, 0.0f, 0.0f)

                val CODEC: Codec<FacingVector> = RecordCodecBuilder.create { ins ->
                    ins.group(
                        AllRegisters.Codecs.VEC3F.fieldOf("view").forGetter(FacingVector::view),
                        AllRegisters.Codecs.VEC3F.fieldOf("left").forGetter(FacingVector::left),
                        AllRegisters.Codecs.VEC3F.fieldOf("up").forGetter(FacingVector::up),
                        Codec.FLOAT.fieldOf("scale").forGetter(FacingVector::scale),
                        AllRegisters.Codecs.VEC3F.fieldOf("offset").forGetter(FacingVector::offset),
                    ).apply(ins) { view, left, up, scale, offset ->
                        FacingVector().also { locate ->
                            locate.view.set(view)
                            locate.left.set(left)
                            locate.up.set(up)
                            locate.scale = scale
                            locate.offset.set(offset)
                        }
                    }
                }
            }
        }
    }

    class Screen(val position: Vector2f, var arrange: Boolean = true) : LocateType {
        operator fun component1() = position.x
        operator fun component2() = position.y

        fun setPosition(x: Float, y: Float) {
            position.set(x, y)
        }

        override fun getScreenSpacePosition(context: HologramContext, partialTick: Float): ScreenPosition =
            ScreenPosition.of(position.x, position.y)

        companion object {
            val CODEC: Codec<Screen> = RecordCodecBuilder.create { ins ->
                ins.group(
                    AllRegisters.Codecs.VEC2F.fieldOf("position").forGetter(Screen::position),
                    Codec.BOOL.fieldOf("arrange").forGetter(Screen::arrange)
                ).apply(ins, ::Screen)
            }
        }
    }

    companion object {
        val CODEC = object : Codec<LocateType> {
            override fun <T : Any> encode(
                input: LocateType,
                ops: DynamicOps<T>,
                prefix: T
            ): DataResult<T> {
                val typeInt: Int
                val codec = when (input) {
                    is Screen -> {
                        typeInt = 1
                        Screen.CODEC
                    }

                    World.FacingPlayer -> {
                        typeInt = 2
                        World.FacingPlayer.CODEC
                    }

                    is World.FacingVector -> {
                        typeInt = 3
                        World.FacingPlayer.CODEC
                    }
                }
                val map = ops.emptyMap()
                ops.set(map, "type", ops.createInt(typeInt))
                val content = ops.withEncoder(codec).apply(input.unsafeCast())
                if (content.isError) return DataResult.error { "error while encode content" }
                ops.set(map, "content", content.orThrow)
                return DataResult.success(map)
            }

            override fun <T : Any> decode(
                ops: DynamicOps<T>,
                input: T
            ): DataResult<Pair<LocateType, T>> = ops.getMap(input).flatMap {
                val numberValue = ops.getNumberValue(it.get("type"))
                if (numberValue.isError) return@flatMap DataResult.error { "" }
                val codec = when (numberValue.orThrow) {
                    1 -> Screen.CODEC
                    2 -> World.FacingPlayer.CODEC
                    3 -> World.FacingPlayer.CODEC
                    else -> throw RuntimeException()
                }
                val content = it.get("content")
                codec.decode(ops, content)
                TODO()
            }
        }
    }
}
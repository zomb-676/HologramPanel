package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.ScreenCoordinate
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f

sealed interface LocateType {

    fun getScreenSpacePosition(): ScreenCoordinate

    sealed class WorldToScreen : LocateType {
        private val internalPosition: Vector4f = Vector4f()
        private val resStore: Vector4f = Vector4f()

        fun setPosition(position: Vector3fc) {
            this.internalPosition.x = position.x()
            this.internalPosition.y = position.y()
            this.internalPosition.z = position.z()
        }

        fun setPosition(vec3: Vec3i) {
            this.internalPosition.x = vec3.x.toFloat()
            this.internalPosition.y = vec3.y.toFloat()
            this.internalPosition.z = vec3.z.toFloat()
        }

        fun setPosition(vec3: Vec3) {
            this.internalPosition.x = vec3.x.toFloat()
            this.internalPosition.y = vec3.y.toFloat()
            this.internalPosition.z = vec3.z.toFloat()
        }

        override fun getScreenSpacePosition(): ScreenCoordinate =
            MVPMatrixRecorder.transform(internalPosition)
    }

    sealed interface PositionType : LocateType

    sealed class World : PositionType, WorldToScreen() {
        class FacingPlayer() : World()
        class FacingVector(val direction: Vector3f) : World()
    }

    sealed interface Screen : PositionType {
        data class ByScreenSpace(val position: Vector2f) : Screen {
            override fun getScreenSpacePosition(): ScreenCoordinate = ScreenCoordinate.of(position)
        }

        class ByWorldSpace : Screen, WorldToScreen()

        object Free : Screen {
            override fun getScreenSpacePosition() = throw RuntimeException()
        }
    }
}
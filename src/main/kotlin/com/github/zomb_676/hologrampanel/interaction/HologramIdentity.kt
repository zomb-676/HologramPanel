package com.github.zomb_676.hologrampanel.interaction

import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import org.joml.Vector3f
import org.joml.Vector3fc

interface HologramIdentity {
    override operator fun equals(other: Any?): Boolean
    override fun hashCode(): Int

    /**
     * the world position the hologram located in world
     *
     *  the return value should not be modified
     */
    fun hologramCenterPosition(): Vector3fc

    /**
     * for entity should consider interpolation
     */
    fun hologramCenterPosition(partialTick: Float): Vector3fc

    class EntitySource(val entity: Entity) : HologramIdentity {
        override fun equals(other: Any?): Boolean = entity == other

        override fun hashCode(): Int = entity.hashCode()

        override fun hologramCenterPosition(): Vector3fc =
            Vector3f(entity.x.toFloat(), entity.y.toFloat() + (entity.bbHeight), entity.z.toFloat())

        override fun hologramCenterPosition(partialTick: Float): Vector3fc {
            val value = partialTick.toDouble()
            val x = Mth.lerp(value, entity.xOld, entity.x)
            val y = Mth.lerp(value, entity.yOld, entity.y)
            val z = Mth.lerp(value, entity.zOld, entity.z)
            return Vector3f(x.toFloat(), y.toFloat() + entity.bbHeight, z.toFloat())
        }
    }

    class PositionSource(val pos: BlockPos) : HologramIdentity {
        private val centerPosition = Vector3f(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f)

        override fun equals(other: Any?): Boolean = pos == other
        override fun hashCode(): Int = pos.hashCode()

        override fun hologramCenterPosition(): Vector3fc = centerPosition
        override fun hologramCenterPosition(partialTick: Float): Vector3fc = centerPosition
    }

    class SimpleMergedClassPos(vararg val pos: BlockPos) : HologramIdentity {
        val centerPos: Vector3fc = run {
            var x = 0
            var y = 0
            var z = 0
            for (i in pos) {
                x += i.x
                y += i.y
                z += i.z
            }
            val count = pos.size.toFloat()
            Vector3f(x / count, y / count, z / count)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SimpleMergedClassPos) return false
            if (!pos.contentEquals(other.pos)) return false
            return true
        }

        override fun hashCode(): Int {
            return pos.contentHashCode()
        }

        override fun hologramCenterPosition(): Vector3fc = centerPos
        override fun hologramCenterPosition(partialTick: Float): Vector3fc = centerPos
    }
}
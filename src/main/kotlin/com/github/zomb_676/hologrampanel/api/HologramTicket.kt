package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import net.minecraft.client.Minecraft

@FunctionalInterface
interface HologramTicket<T : HologramContext> {
    /**
     * this is guaranteed to be called every client tick
     */
    fun stillValid(context: T): Boolean

    /**
     * any critical false will be considered invalid
     *
     * then, any none-critical ture will be considered valid
     */
    @EfficientConst
    fun isCritical(): Boolean = true

    data class ByDistance<T : HologramContext>(val distance: Double) : HologramTicket<T> {
        override fun stillValid(context: T): Boolean {
            val pos = context.hologramCenterPosition()
            val playerPos = Minecraft.getInstance().player?.position() ?: return false
            val dis = pos.distance(playerPos.x.toFloat(), playerPos.y.toFloat(), playerPos.z.toFloat())
            return dis <= distance
        }
    }

    class ByTick<T : HologramContext>(tick: Int) : HologramTicket<T> {
        init {
            require(tick > 0)
        }

        var tick: Int = tick
            private set

        override fun stillValid(context: T): Boolean = --tick == 0
    }
}
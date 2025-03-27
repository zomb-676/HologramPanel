package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import net.minecraft.client.Minecraft

@FunctionalInterface
interface HologramTicket<in T : HologramContext> {
    /**
     * this is guaranteed to be called every client tick
     */
    fun stillValid(context: T, state: HologramRenderState): Boolean

    /**
     * any critical false will be considered invalid
     *
     * then, any none-critical ture will be considered valid
     */
    @EfficientConst
    fun isCritical(): Boolean = true

    companion object {
        fun byPopUpDistance() = ByDistance(Config.Client.popUpDistance.get().toDouble())
    }

    data class ByDistance(val distance: Double) : HologramTicket<HologramContext> {
        override fun stillValid(context: HologramContext, state: HologramRenderState): Boolean {
            val pos = context.hologramCenterPosition()
            val playerPos = Minecraft.getInstance().player?.position() ?: return false
            val dis = pos.distance(playerPos.x.toFloat(), playerPos.y.toFloat(), playerPos.z.toFloat())
            return dis <= distance
        }
    }

    /**
     * the generic parameter type is useless here
     */
    class ByTick(tick: Int) : HologramTicket<HologramContext> {
        init {
            require(tick > 0)
        }

        var tick: Int = tick
            private set

        override fun stillValid(context: HologramContext, state: HologramRenderState): Boolean = --tick > 0
        override fun toString(): String {
            return "ByTick(tick=$tick)"
        }
    }

    data object BySee : HologramTicket<HologramContext> {
        override fun stillValid(context: HologramContext, state: HologramRenderState): Boolean = state.isLookingAt()
    }

    class ByTickAfterNotSee(val aliveTick : Int) : HologramTicket<HologramContext> {
        init {
            require(aliveTick > 0)
        }

        var tick: Int = aliveTick
            private set

        override fun stillValid(context: HologramContext, state: HologramRenderState): Boolean {
            if (state.isLookingAt()) {
                this.tick = this.aliveTick
            }
            return --tick > 0
        }

        override fun toString(): String {
            return "ByTickAfterNotSee(tick=$tick)"
        }
    }
}
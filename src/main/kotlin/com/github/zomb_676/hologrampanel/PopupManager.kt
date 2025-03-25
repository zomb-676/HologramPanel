package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.RayTraceHelper
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.AutoTicker
import com.github.zomb_676.hologrampanel.util.profilerStack
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

object PopupManager {
    val tick = AutoTicker.by(Config.Client.popUpInterval::get)

    fun tickPopup() = tick.tryConsume {
        profilerStack("hologram_pop_up") {
            val player: LocalPlayer = Minecraft.getInstance().player ?: return@profilerStack
            val level: Level = player.level()
            val radius = Config.Client.popUpDistance.get().toDouble()

            val aabb = AABB.ofSize(player.position(), radius, radius, radius)

            val manager = PluginManager.getInstance()
            for (pos in BlockPos.betweenClosed(aabb)) {
                if (HologramManager.checkIdentityExist(pos)) continue
                val popup = manager.popUpBlock(pos, level) ?: continue
                val context = BlockHologramContext(pos.immutable(), player, null)
                tryAdd(context)
            }
            for (entity in level.getEntities(null, aabb)) {
                if (entity == Minecraft.getInstance().player) continue
                if (HologramManager.checkIdentityExist(entity.uuid)) continue
                val popup = manager.popUpEntity(entity) ?: continue
                val context = EntityHologramContext(entity, player, null)
                tryAdd(context)
            }
        }
    }

    private fun tryAdd(context: HologramContext) {
        val widget = RayTraceHelper.createHologramWidget(context) ?: return
        HologramManager.tryAddWidget(widget, context)
    }

}
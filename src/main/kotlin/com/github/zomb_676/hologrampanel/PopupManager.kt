package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.HologramTicket
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.RayTraceHelper
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.AutoTicker
import com.github.zomb_676.hologrampanel.util.profilerStack
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.DisplayType
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

/**
 * object class which is designed to do popup
 */
data object PopupManager {
    val tick = AutoTicker.by(Config.Client.popUpInterval::get)

    fun tickPopup() = tick.tryConsume {
        profilerStack("hologram_pop_up") {
            val player: LocalPlayer = Minecraft.getInstance().player ?: return@profilerStack
            val level: Level = player.level()
            val radius = Config.Client.popUpDistance.get().toDouble()

            val aabb = AABB.ofSize(player.position(), radius, radius, radius)

            val manager = PluginManager.getInstance()
            for (pos: BlockPos in BlockPos.betweenClosed(aabb)) {
                if (HologramManager.checkIdentityExist(pos)) continue
                val ticket = manager.popUpBlock(pos, level)
                if (ticket.isEmpty()) continue
                val context = BlockHologramContext(pos.immutable(), player)
                val state = tryAdd(context, DisplayType.NORMAL, ticket.unsafeCast()) ?: continue
                DebugHelper.Client.recordPopup(state)
            }
            for (entity: Entity in level.getEntities(null, aabb)) {
                if (entity == Minecraft.getInstance().player) continue
                if (HologramManager.checkIdentityExist(entity.uuid)) continue
                val ticket = manager.popUpEntity(entity)
                if (ticket.isEmpty()) continue
                val context = EntityHologramContext(entity, player)
                val state = tryAdd(context, DisplayType.NORMAL, ticket.unsafeCast()) ?: continue
                DebugHelper.Client.recordPopup(state)
            }
        }
    }

    private fun tryAdd(
        context: HologramContext,
        displayType: DisplayType,
        ticket: List<HologramTicket<HologramContext>>
    ): HologramRenderState? {
        val widget = RayTraceHelper.createHologramWidget(context, displayType) ?: return null
        return HologramManager.tryAddWidget(widget, context, DisplayType.NORMAL, ticket)
    }
}
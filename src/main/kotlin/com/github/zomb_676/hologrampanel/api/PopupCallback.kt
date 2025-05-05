package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

sealed interface PopupCallback {
    fun interface BlockPopupCallback {
        /**
         * @return none empty list will pop up the hologram
         */
        fun popup(pos: BlockPos,blockState: BlockState, level: Level): List<HologramTicket<BlockHologramContext>>
    }

    fun interface EntityPopupCallback {
        /**
         * @return none empty list will pop up the hologram
         */
        fun popup(entity: Entity): List<HologramTicket<EntityHologramContext>>
    }
}
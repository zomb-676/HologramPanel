package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level

sealed interface PopupCallback {
    fun interface BlockPopupCallback {
        fun popup(pos : BlockPos, level : Level) : List<HologramTicket<BlockHologramContext>>
    }

    fun interface EntityPopupCallback {
        fun popup(entity : Entity) : List<HologramTicket<EntityHologramContext>>
    }
}
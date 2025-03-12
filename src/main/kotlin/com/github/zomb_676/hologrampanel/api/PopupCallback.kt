package com.github.zomb_676.hologrampanel.api

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level

sealed interface PopupCallback {
    fun interface BlockPopupCallback {
        fun popup(pos : BlockPos, level : Level) : PopupType?
    }

    fun interface EntityPopupCallback {
        fun popup(entity : Entity) : PopupType?
    }
}
package com.github.zomb_676.hologrampanel.api

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import java.util.function.Predicate

class HologramClientRegistration(internal val plugin: IHologramPlugin) {
    internal val blockPopup: MutableList<PopupCallback.BlockPopupCallback> = mutableListOf()
    internal val entityPopup: MutableList<PopupCallback.EntityPopupCallback> = mutableListOf()
    internal val hideBlocks: MutableSet<Block> = mutableSetOf()
    internal val hideEntityTypes: MutableSet<EntityType<*>> = mutableSetOf()
    internal val hideEntityCallback: MutableSet<Predicate<Entity>> = mutableSetOf()

    fun registerBlockPopupCallback(popupCallback: PopupCallback.BlockPopupCallback) {
        blockPopup.add(popupCallback)
    }

    fun registerEntityPopupCallback(popupCallback: PopupCallback.EntityPopupCallback) {
        entityPopup.add(popupCallback)
    }

    fun hideBlock(block: Block) {
        hideBlocks.add(block)
    }

    fun hideEntity(entityType: EntityType<*>) {
        hideEntityTypes.add(entityType)
    }

    fun hideEntity(code: Predicate<Entity>) {
        hideEntityCallback.add(code)
    }
}
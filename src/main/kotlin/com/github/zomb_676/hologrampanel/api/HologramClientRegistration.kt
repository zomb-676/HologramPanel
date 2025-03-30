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

    /**
     * register popup callback for blocks
     */
    fun registerBlockPopupCallback(popupCallback: PopupCallback.BlockPopupCallback) {
        blockPopup.add(popupCallback)
    }

    /**
     * register popup for entities
     */
    fun registerEntityPopupCallback(popupCallback: PopupCallback.EntityPopupCallback) {
        entityPopup.add(popupCallback)
    }

    /**
     * @param block interrupt the creation chain if target this block
     */
    fun hideBlock(block: Block) {
        hideBlocks.add(block)
    }

    /**
     * @param entityType interrupt the creation chain if target this entity
     */
    fun hideEntity(entityType: EntityType<*>) {
        hideEntityTypes.add(entityType)
    }

    /**
     * @param code interrupt the creation chain if target this entity
     */
    fun hideEntity(code: Predicate<Entity>) {
        hideEntityCallback.add(code)
    }
}
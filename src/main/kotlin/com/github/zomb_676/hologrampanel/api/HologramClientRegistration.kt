package com.github.zomb_676.hologrampanel.api

import net.minecraft.world.level.block.Block
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
class HologramClientRegistration(internal val plugin: IHologramPlugin) {
    internal val blockPopup: MutableList<PopupCallback.BlockPopupCallback> = mutableListOf()
    internal val entityPopup: MutableList<PopupCallback.EntityPopupCallback> = mutableListOf()
    internal val hideBlocks: MutableSet<Block> = mutableSetOf()

    fun registerBlockPopupCallback(popupCallback: PopupCallback.BlockPopupCallback) {
        blockPopup.add(popupCallback)
    }

    fun registerEntityPopupCallback(popupCallback: PopupCallback.EntityPopupCallback) {
        entityPopup.add(popupCallback)
    }

    fun hideBlock(block: Block) {
        hideBlocks.add(block)
    }
}
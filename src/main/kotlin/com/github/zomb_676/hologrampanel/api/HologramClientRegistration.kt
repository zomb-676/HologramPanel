package com.github.zomb_676.hologrampanel.api

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
class HologramClientRegistration(internal val plugin: IHologramPlugin) {
    internal val blockPopup : MutableList<PopupCallback.BlockPopupCallback> = mutableListOf()
    internal val entityPopup : MutableList<PopupCallback.EntityPopupCallback> = mutableListOf()

    fun registerBlockPopupCallback(popupCallback: PopupCallback.BlockPopupCallback) {
        blockPopup.add(popupCallback)
    }

    fun registerEntityPopupCallback(popupCallback: PopupCallback.EntityPopupCallback) {
        entityPopup.add(popupCallback)
    }
}
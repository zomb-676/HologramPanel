package com.github.zomb_676.hologrampanel.api

sealed interface PopupType {
    data class PlayerSee(val disappearDelay: Int) : PopupType
    data class AutoPopup(val disappearDistance: Double) : PopupType
    data class Trig<T>(val source: T) : PopupType
}
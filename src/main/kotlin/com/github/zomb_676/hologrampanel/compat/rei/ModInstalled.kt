package com.github.zomb_676.hologrampanel.compat.rei

object ModInstalled {
    private var reiInstalled = false

    fun reiInstalled() {
        reiInstalled = true
    }

    fun isReiInstalled() = reiInstalled
}
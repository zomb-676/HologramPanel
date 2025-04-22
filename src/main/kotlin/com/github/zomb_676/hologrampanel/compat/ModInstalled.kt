package com.github.zomb_676.hologrampanel.compat

import net.neoforged.fml.ModList

object ModInstalled {
    val reiInstalled = ModList.get().isLoaded("rei")
    val jeiInstalled = ModList.get().isLoaded("jei")
}
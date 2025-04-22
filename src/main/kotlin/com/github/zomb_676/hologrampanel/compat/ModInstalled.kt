package com.github.zomb_676.hologrampanel.compat

import net.neoforged.fml.ModList

@Suppress("SpellCheckingInspection")
object ModInstalled {
    val reiInstalled = ModList.get().isLoaded("roughlyenoughitems")
    val jeiInstalled = ModList.get().isLoaded("jei")
}
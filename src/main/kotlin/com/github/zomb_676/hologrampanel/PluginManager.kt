package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.HologramCommonRegistration
import com.github.zomb_676.hologrampanel.api.IHologramPlugin

internal class PluginManager private constructor(val plugins: List<IHologramPlugin>) {
    companion object {
        private var INSTANCE: PluginManager? = null

        fun getInstance() = INSTANCE!!

        fun init(plugins: List<IHologramPlugin>) {
            INSTANCE = PluginManager(plugins)
        }
    }

    val commonRegistration: Map<IHologramPlugin, HologramCommonRegistration> =
        plugins.associateWith { HologramCommonRegistration(it) }
}
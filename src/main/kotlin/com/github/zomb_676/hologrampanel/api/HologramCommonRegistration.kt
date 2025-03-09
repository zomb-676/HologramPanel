package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider

class HologramCommonRegistration(val plugin: IHologramPlugin) {

    internal val blockProviders: MutableList<ComponentProvider<BlockHologramContext>> = mutableListOf()

    fun registerBlockComponent(provider: ComponentProvider<BlockHologramContext>) {
        blockProviders.add(provider)
    }
}
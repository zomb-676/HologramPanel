package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetBuilder
import net.minecraft.resources.ResourceLocation

class HologramCommonRegistration(val plugin: IHologramPlugin) {

    internal val blockProviders: MutableList<ComponentProvider<BlockHologramContext>> = mutableListOf()

    fun registerBlockComponent(provider: ComponentProvider<BlockHologramContext>) {
        blockProviders.add(provider)
    }

    inline fun <reified T> registerBlockComponent(
        name: String,
        crossinline f: (BlockHologramContext).(HologramWidgetBuilder<BlockHologramContext>) -> Unit
    ) {
        val provider = object : ComponentProvider<BlockHologramContext> {
            override fun appendComponent(builder: HologramWidgetBuilder<BlockHologramContext>) {
                f.invoke(builder.context, builder)
            }

            override fun targetClass(): Class<*> = T::class.java

            override fun location(): ResourceLocation = plugin.location().withPath(name)
        }
        registerBlockComponent(provider)
    }
}
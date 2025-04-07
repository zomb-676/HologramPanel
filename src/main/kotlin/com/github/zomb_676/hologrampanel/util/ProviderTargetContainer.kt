package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.PluginManager
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.DefaultProvider
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramWorldContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder

class ProviderTargetContainer<C : HologramContext>(val context: C) {

    private val internalData: MutableList<Pair<ComponentProvider<C, *>, (C) -> Any>> = mutableListOf()

    data class Ins<C : HologramContext, T : Any>(val f: (C) -> T, val providers: List<ComponentProvider<C, T>>)

    private var data: MutableList<Ins<C, *>> = mutableListOf()
    private var inverseMap: MutableMap<ComponentProvider<C, *>, (C) -> Any> = mutableMapOf()

    fun addSource(f: (C) -> Any?) {
        val targetIns = f.invoke(context)
        if (targetIns == null) return
        val providers = PluginManager.ProviderManager.collectByInstance(context, targetIns)
            .asSequence()
            .filter { provider ->
                provider.unsafeCast<ComponentProvider<C, Any>>().appliesToByInstance(context, targetIns)
            }.map { it to f.unsafeCast<(C) -> Any>() }.forEach(internalData::add)
    }

    private fun removeByPrevent(container: MutableList<Pair<ComponentProvider<C, *>, (C) -> Any>>): MutableList<Pair<ComponentProvider<C, *>, (C) -> Any>> {
        if (container.size <= 1) return container
        val backup = container.toMutableList()
        val iterator = container.listIterator()
        while (iterator.hasNext()) {
            val ins = iterator.next()
            val location = ins.first.location()
            if (backup.any { it.first.replaceProvider(location) }) {
                backup.remove(ins)
                iterator.remove()
            }
        }
        return container
    }

    fun doPreventRemove() {
        inverseMap.clear()
        data.clear()
        removeByPrevent(internalData).groupBy({ it.second }, { it.first }).forEach { (f, providers) ->
            data += Ins(f, providers.unsafeCast())
            providers.forEach {
                inverseMap[it] = f
            }
        }
    }

    fun isEmpty() = data.isEmpty()

    fun providers(): List<Ins<C, *>> = data

    fun <V> getDataForProvider(provider: ComponentProvider<C, V>): V {
        if (provider is DefaultProvider) {
            return provider.getDefaultTarget(context)
        }
        val f = inverseMap[provider] ?: throw RuntimeException()
        val data = f.invoke(context)
        return data.unsafeCast<V>()
    }

    fun <V> appendProviderComponent(provider: ComponentProvider<C, V>, builder: HologramWidgetBuilder<C>, displayType: DisplayType) {
        val data = getDataForProvider(provider)
        provider.appendComponent(data, builder, displayType)
    }
}
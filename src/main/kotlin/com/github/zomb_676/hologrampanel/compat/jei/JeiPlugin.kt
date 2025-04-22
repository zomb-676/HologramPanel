package com.github.zomb_676.hologrampanel.compat.jei

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.util.SearchBackend
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

@JeiPlugin
class JeiPlugin : IModPlugin {
    companion object {
        val PLUGIN_LOCATION = HologramPanel.rl("jei_plugin")
        var available: Boolean = false
        private var jeiSearchEngine: SearchBackend? = null

        fun getSearchEngine(): SearchBackend? {
            if (!available) return null
            val engine = jeiSearchEngine ?: return null
            if (!engine.available()) return null
            return engine
        }
    }

    override fun getPluginUid(): ResourceLocation = PLUGIN_LOCATION

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        available = true
        val filter = jeiRuntime.ingredientFilter
        jeiSearchEngine = object : SearchBackend {
            private var lastFilterText: String = ""
            private var cached: Lazy<List<ItemStack>> = lazy { listOf() }
            private var setCache: Lazy<Set<Item>> = lazy { emptySet() }
            override fun available(): Boolean = available
            override fun matches(item: ItemStack): Boolean = setCache.value.contains(item.item)
            override fun getSearchString(): String = filter.filterText
            override fun setSearchString(searchString: String): Boolean {
                if (searchString != lastFilterText) {
                    filter.filterText = searchString
                    update()
                }
                return true
            }

            fun update() {
                cached = lazy { filter.filteredItemStacks }
                setCache = lazy { cached.value.asSequence().map { it.item }.toSet() }
            }
        }
    }

    override fun onRuntimeUnavailable() {
        available = false
        jeiSearchEngine = null
    }
}

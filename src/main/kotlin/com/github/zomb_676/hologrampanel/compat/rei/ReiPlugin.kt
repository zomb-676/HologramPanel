package com.github.zomb_676.hologrampanel.compat.rei

import com.github.zomb_676.hologrampanel.util.SearchBackend
import me.shedaniel.rei.api.client.REIRuntime
import me.shedaniel.rei.api.client.plugins.REIClientPlugin
import me.shedaniel.rei.api.common.util.EntryStacks
import me.shedaniel.rei.forge.REIPluginClient
import me.shedaniel.rei.impl.client.gui.widget.entrylist.EntryListSearchManager
import net.minecraft.world.item.ItemStack

/**
 * tne rei plugin which supports search by its search engine
 */
@REIPluginClient
class ReiPlugin : REIClientPlugin {
    init {
        ModInstalled.reiInstalled()
    }

    object ReiSearchBackend : SearchBackend {
        override fun available(): Boolean = REIRuntime.getInstance() != null

        override fun matches(item: ItemStack): Boolean {
            val filter = EntryListSearchManager.INSTANCE.searchManager.filter ?: return true
            val entry = EntryStacks.of(item)
            return filter.test(entry)
        }

        override fun getSearchString(): String? = REIRuntime.getInstance().searchTextField?.text

        override fun setSearchString(searchString: String): Boolean {
            val field = REIRuntime.getInstance().searchTextField
            if (field != null) {
                field.text = searchString
                return true
            } else {
                return false
            }
        }

    }
}
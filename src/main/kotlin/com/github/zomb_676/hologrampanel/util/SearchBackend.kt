package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.compat.ModInstalled
import com.github.zomb_676.hologrampanel.compat.jei.JeiPlugin
import com.github.zomb_676.hologrampanel.compat.rei.ReiPlugin
import net.minecraft.world.item.ItemStack

/**
 * a simple engine to filter objects by String
 *
 * should check [Type.isAvailable] and [SearchBackend.available]
 */
interface SearchBackend {
    enum class Type {
        DEFAULT {
            override fun isAvailable(): Boolean = true
            override fun getSearchBackend(): SearchBackend = Default
        },
        REI {
            override fun isAvailable(): Boolean = ModInstalled.reiInstalled
            override fun getSearchBackend(): SearchBackend? {
                if (!isAvailable()) return null
                return ReiPlugin.getSearchEngine()
            }
        },
        JEI {
            override fun isAvailable(): Boolean = ModInstalled.jeiInstalled
            override fun getSearchBackend(): SearchBackend? {
                if (!isAvailable()) return null
                return JeiPlugin.getSearchEngine()
            }
        },
        AUTO {
            override fun isAvailable(): Boolean = true
            override fun getSearchBackend(): SearchBackend? =
                JEI.getSearchBackend() ?: REI.getSearchBackend() ?: DEFAULT.getSearchBackend()
        };

        /**
         * indicates the target mod is installed or not
         */
        abstract fun isAvailable(): Boolean
        abstract fun getSearchBackend(): SearchBackend?
    }

    companion object {
        fun getCurrentBackend(): SearchBackend {
            val backend = Config.Client.searchBackend.get().getSearchBackend()
            if (backend != null) return backend
            return Default
        }
    }

    /**
     * if this engine is available or not
     */
    fun available(): Boolean
    fun matches(item: ItemStack): Boolean

    fun getSearchString(): String?
    fun setSearchString(searchString: String): Boolean

    object Default : SearchBackend {
        var searchText: String = ""
        override fun available(): Boolean = true

        override fun matches(item: ItemStack): Boolean =
            item.displayName.string.contains(searchText, true)

        override fun getSearchString(): String = searchText

        override fun setSearchString(searchString: String): Boolean {
            this.searchText = searchString
            return true
        }
    }
}
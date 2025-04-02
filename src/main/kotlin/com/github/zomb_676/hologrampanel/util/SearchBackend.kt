package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.compat.rei.ModInstalled
import com.github.zomb_676.hologrampanel.compat.rei.ReiPlugin
import net.minecraft.world.item.ItemStack

interface SearchBackend {
    enum class Type {
        DEFAULT {
            override fun isAvailable(): Boolean = true
        },
        REI {
            override fun isAvailable(): Boolean = ModInstalled.isReiInstalled()
        },
        AUTO {
            override fun isAvailable(): Boolean = true
        };

        abstract fun isAvailable(): Boolean
    }

    companion object {
        fun getCurrentBackend(): SearchBackend {
            val backend: SearchBackend = when (Config.Client.searchBackend.get()) {
                Type.DEFAULT -> Default
                Type.REI -> if (ModInstalled.isReiInstalled()) ReiPlugin.ReiSearchBackend else Default
                Type.AUTO -> {
                    if (ModInstalled.isReiInstalled()) {
                        ReiPlugin.ReiSearchBackend
                    } else {
                        Default
                    }
                }
            }
            return backend
        }
    }

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
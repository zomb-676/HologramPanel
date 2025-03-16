package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.HologramPanel.Companion.LOGGER
import com.github.zomb_676.hologrampanel.api.*
import com.github.zomb_676.hologrampanel.util.getClassOf
import net.neoforged.fml.ModList
import org.jetbrains.annotations.ApiStatus
import java.lang.annotation.ElementType
import kotlin.streams.asSequence

@ApiStatus.Internal
internal class PluginManager private constructor(val plugins: List<IHologramPlugin>) {
    companion object {
        private var INSTANCE: PluginManager? = null
        private val providerCache: MutableMap<Class<*>, List<ComponentProvider<*>>> = mutableMapOf()
        private val classProvider: MutableMap<Class<*>, List<ComponentProvider<*>>> = mutableMapOf()

        fun getInstance() = INSTANCE!!

        fun init() {
            if (INSTANCE != null) {
                LOGGER.error("not init plugin manager more than once")
                return
            }

            val plugins = run {
                ModList.get().allScanData.asSequence()
                    .flatMap { it.getAnnotatedBy(HologramPlugin::class.java, ElementType.TYPE).asSequence() }.map {
                        var plugin: IHologramPlugin? = null
                        try {

                            val classInstance = getClassOf<IHologramPlugin>(it.clazz().className)
                            val annotation = classInstance.getAnnotation(HologramPlugin::class.java)!!
                            if (annotation.enable && annotation.requireMods.all(ModList.get()::isLoaded)) {
                                plugin = classInstance.getDeclaredConstructor()
                                    .apply { require(trySetAccessible()) }.newInstance()
                                LOGGER.debug("success loaded plugin: {}", plugin.location())
                            } else {
                                LOGGER.debug("skip disabled plugin: {}", classInstance.name)
                            }
                        } catch (e: Exception) {
                            LOGGER.error("failed to load plugin class:{}", it.clazz.className)
                            LOGGER.traceExit(e)
                        }
                        plugin
                    }.filterNotNull().toList()
            }
            INSTANCE = PluginManager(plugins)
        }

        fun onLoadComplete() {
            classProvider.clear()
            classProvider.putAll(AllRegisters.ComponentHologramProviderRegistry.REGISTRY.groupBy { it.targetClass() })
        }

        internal fun queryProvidersForClass(target: Class<*>): List<ComponentProvider<*>> {
            val res = providerCache[target]
            if (res != null) return res

            val list = mutableListOf<ComponentProvider<*>>()
            searchByInheritTree(target, classProvider, list)
            providerCache[target] = list
            return list
        }

        private fun <V> searchByInheritTree(c: Class<*>, map: Map<Class<*>, List<V>>, list: MutableList<V>) {
            val target = map[c]
            if (target != null) {
                list.addAll(target)
            }
            c.interfaces.forEach {
                searchByInheritTree(it, map, list)
            }
            val sup = c.superclass
            if (sup != null) {
                searchByInheritTree(sup, map, list)
            }
        }
    }

    val commonRegistration: Map<IHologramPlugin, HologramCommonRegistration> =
        plugins.associateWith { HologramCommonRegistration(it) }
    val clientRegistration: Map<IHologramPlugin, HologramClientRegistration> =
        plugins.associateWith { HologramClientRegistration(it) }

    val block: MutableList<PopupCallback.BlockPopupCallback> = mutableListOf()
    val entity: MutableList<PopupCallback.EntityPopupCallback> = mutableListOf()

    internal fun onClientRegisterEnd() {
        this.block.addAll(clientRegistration.values.asSequence().flatMap { it.blockPopup })
        this.entity.addAll(clientRegistration.values.asSequence().flatMap { it.entityPopup })
    }
}
package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.HologramPanel.Companion.LOGGER
import com.github.zomb_676.hologrampanel.api.*
import com.github.zomb_676.hologrampanel.util.getClassOf
import net.neoforged.fml.ModList
import java.lang.annotation.ElementType
import kotlin.streams.asSequence

internal class PluginManager private constructor(val plugins: List<IHologramPlugin>) {
    companion object {
        private var INSTANCE: PluginManager? = null

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
                            if (annotation.enable) {
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
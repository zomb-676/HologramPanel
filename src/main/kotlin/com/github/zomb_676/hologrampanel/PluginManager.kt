package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.HologramPanel.Companion.LOGGER
import com.github.zomb_676.hologrampanel.api.*
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.getClassOf
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.neoforged.fml.ModList
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.common.ModConfigSpec
import org.jetbrains.annotations.ApiStatus
import java.lang.annotation.ElementType
import java.util.function.Predicate
import kotlin.jvm.optionals.getOrNull
import kotlin.streams.asSequence

@ApiStatus.Internal
internal class PluginManager private constructor(val plugins: List<IHologramPlugin>) {
    companion object {
        private var INSTANCE: PluginManager? = null
        private val providerCache: MutableMap<Class<*>, List<ComponentProvider<*, *>>> = mutableMapOf()
        private val classProvider: MutableMap<Class<*>, List<ComponentProvider<*, *>>> = mutableMapOf()

        internal fun getInstance() = INSTANCE!!

        internal fun init() {
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
                                plugin = classInstance.getDeclaredConstructor().apply { require(trySetAccessible()) }
                                    .newInstance()
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

        internal fun onLoadComplete() {
            classProvider.clear()
            classProvider.putAll(AllRegisters.ComponentHologramProviderRegistry.REGISTRY.groupBy { it.targetClass() })
        }

        internal fun queryProviders(context: BlockHologramContext): List<ComponentProvider<BlockHologramContext, *>> {
            val list: MutableList<ComponentProvider<BlockHologramContext, *>> = mutableListOf()
            list.addAll(queryProvidersByType(context, context.getBlockState().block).unsafeCast())
            list.addAll(queryProvidersByType(context, context.getFluidState().fluidType).unsafeCast())
            list.addAll(queryProvidersByType(context, context.getBlockEntity()).unsafeCast())
            return removeByPrevent(list)
        }

        internal fun queryProviders(context: EntityHologramContext): List<ComponentProvider<EntityHologramContext, *>> {
            val list: MutableList<ComponentProvider<EntityHologramContext, *>> = mutableListOf()
            list.addAll(queryProvidersByType(context, context.getEntity()).unsafeCast())
            return removeByPrevent(list)
        }

        private fun <T : Any?, C : HologramContext> queryProvidersByType(
            context: C, targetInstance: T
        ): List<ComponentProvider<*, T>> {
            if (targetInstance == null) return listOf()

            val targetClass: Class<out T> = targetInstance::class.java
            val res = providerCache[targetClass]
            if (res != null) return res.unsafeCast()

            val container = mutableListOf<ComponentProvider<C, T>>()
            searchByInheritTree(targetClass, classProvider, container.unsafeCast())
            container.removeIf { !it.appliesTo(context, targetInstance) }
            val finalRes = if (container.isEmpty()) {
                listOf()
            } else if (container.size == 1) {
                container
            } else {
                removeByPrevent(container)
            }
            providerCache[targetClass] = finalRes
            return finalRes
        }

        private fun <T : ComponentProvider<*, *>> removeByPrevent(container: MutableList<T>): MutableList<T> {
            if (container.size <= 1) return container
            val backup = container.toMutableList()
            val iterator = container.listIterator()
            while (iterator.hasNext()) {
                val provider = iterator.next()
                val location = provider.location()
                if (backup.any { it.replaceProvider(location) }) {
                    backup.remove(provider)
                    iterator.remove()
                }
            }
            return container
        }

        private fun <V> searchByInheritTree(
            c: Class<*>, map: Map<Class<*>, List<V>>, list: MutableList<V>
        ) where V : ComponentProvider<*, *> {
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

    internal val commonRegistration: Map<IHologramPlugin, HologramCommonRegistration> =
        plugins.associateWith { HologramCommonRegistration(it) }
    internal val clientRegistration: Map<IHologramPlugin, HologramClientRegistration> =
        plugins.associateWith { HologramClientRegistration(it) }

    internal val block: MutableList<PopupCallback.BlockPopupCallback> = mutableListOf()
    internal val entity: MutableList<PopupCallback.EntityPopupCallback> = mutableListOf()

    internal val hideBlocks: MutableSet<Block> = mutableSetOf()

    internal val hideEntityTypes: MutableSet<EntityType<*>> = mutableSetOf()
    internal val hideEntityCallback: MutableSet<Predicate<Entity>> = mutableSetOf()

    internal fun onClientRegisterEnd() {
        this.block.addAll(clientRegistration.values.asSequence().flatMap(HologramClientRegistration::blockPopup))
        this.entity.addAll(clientRegistration.values.asSequence().flatMap(HologramClientRegistration::entityPopup))
        this.hideBlocks.addAll(clientRegistration.values.asSequence().flatMap(HologramClientRegistration::hideBlocks))
        this.hideEntityTypes.addAll(
            clientRegistration.values.asSequence().flatMap(HologramClientRegistration::hideEntityTypes)
        )
        this.hideEntityCallback.addAll(
            clientRegistration.values.asSequence().flatMap(HologramClientRegistration::hideEntityCallback)
        )

        val configBuilder = ModConfigSpec.Builder()
        plugins.forEach { plugin ->
            configBuilder.stack("plugin:${plugin.location()}") {
                configBuilder.define("enable", true)
                plugin.registerClientConfig(configBuilder)

                commonRegistration[plugin]!!.also { registration ->
                    registration.blockProviders.forEach { provider ->
                        configBuilder.stack("provider:${provider.location()}") {
                            configBuilder.define("enable", true)
                        }
                    }
                    registration.entityProviders.forEach { provider ->
                        configBuilder.stack("provider:${provider.location()}") {
                            configBuilder.define("enable", true)
                        }
                    }
                }
            }
        }
        val container = ModList.get().getModContainerById(HologramPanel.MOD_ID).getOrNull()!!
        container.registerConfig(ModConfig.Type.CLIENT, configBuilder.build(), "hologram_panel_plugin_settings")
    }

    fun hideBlock(block: Block) = this.hideBlocks.contains(block)
    fun hideEntity(entity: Entity) =
        this.hideEntityTypes.contains(entity.type) || this.hideEntityCallback.any { it.test(entity) }
}
package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.HologramPanel.Companion.LOGGER
import com.github.zomb_676.hologrampanel.api.*
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.InheritSearcher
import com.github.zomb_676.hologrampanel.util.getClassOf
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.forgespi.language.ModFileScanData
import org.jetbrains.annotations.ApiStatus
import java.lang.annotation.ElementType
import java.util.function.Predicate
import kotlin.jvm.optionals.getOrNull
import kotlin.sequences.toList

@ApiStatus.Internal
internal class PluginManager private constructor(val plugins: List<IHologramPlugin>) {
    companion object {
        private var INSTANCE: PluginManager? = null

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
                            LOGGER.throwing(e)
                        }
                        plugin
                    }.filterNotNull().toList()
            }
            INSTANCE = PluginManager(plugins)
        }

        private fun ModFileScanData.getAnnotatedBy(type: Class<out Annotation>, elementType: ElementType): Sequence<ModFileScanData.AnnotationData> {
            val t = org.objectweb.asm.Type.getType(type)
            return annotations.asSequence().filter { it.targetType == elementType && it.annotationType == t}
        }

        fun <T, V> flatten(source: Map<*, T>, f: (T) -> Collection<V>): Sequence<V> =
            source.values.asSequence().flatMap(f)

    }

    object ProviderManager {
        private val mapper = InheritSearcher<ComponentProvider<*, *>>()

        private fun <V, C : HologramContext> InheritSearcher<ComponentProvider<*, *>>.collectByInstance(context: C, instance: V) =
            this.collectByInstance(instance) { i, p ->
                p.unsafeCast<ComponentProvider<C, V>>().appliesTo(context, i)
            }

        internal fun collectProvidersFromRegistry() {
            mapper.resetMapper()
            AllRegisters.ComponentHologramProviderRegistry.ID_MAP.forEach { provider ->
                mapper.getMutableMapper().computeIfAbsent(provider.targetClass()) { mutableListOf() }.add(provider)
            }
        }

        internal fun queryProviders(context: BlockHologramContext): List<ComponentProvider<BlockHologramContext, *>> {
            val list: MutableList<ComponentProvider<BlockHologramContext, *>> = mutableListOf()
            list.addAll(mapper.collectByInstance(context, context.getBlockState().block).unsafeCast())
            list.addAll(mapper.collectByInstance(context, context.getFluidState().fluidType).unsafeCast())
            list.addAll(mapper.collectByInstance(context, context.getBlockEntity()).unsafeCast())
            return removeByPrevent(list)
        }

        internal fun queryProviders(context: EntityHologramContext): List<ComponentProvider<EntityHologramContext, *>> {
            val list: MutableList<ComponentProvider<EntityHologramContext, *>> = mutableListOf()
            list.addAll(mapper.collectByInstance(context, context.getEntity()).unsafeCast())
            return removeByPrevent(list)
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

        internal fun invalidateCache() = this.mapper.resetCache()

        internal fun removeProvider(provider: ComponentProvider<*, *>) {
            this.mapper.getMutableMapper()[provider.targetClass()]?.remove(provider)
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

    internal val globalPluginSettingsMap: MutableMap<ForgeConfigSpec, PluginGlobalSetting> = mutableMapOf()
    internal val globalPluginSettings: MutableMap<IHologramPlugin, PluginGlobalSetting> = mutableMapOf()

    internal fun registerPluginConfigs() {
        plugins.forEach { plugin ->
            val configBuilder = ForgeConfigSpec.Builder()
            val pluginEnable = configBuilder.define("${plugin.location()}_enable_plugin", true)
            plugin.registerClientConfig(configBuilder)

            val providerEnableData: MutableMap<ComponentProvider<*, *>, ForgeConfigSpec.BooleanValue> = mutableMapOf()
            commonRegistration[plugin]!!.also { registration ->
                registration.blockProviders.forEach { provider ->
                    val enable = configBuilder.define("${plugin.location()}_${provider.location()}_enable_block", true)
                    providerEnableData.put(provider, enable)
                }
                registration.entityProviders.forEach { provider ->
                    val enable = configBuilder.define("${plugin.location()}_${provider.location()}_enable_entity", true)
                    providerEnableData.put(provider, enable)
                }
            }
            val container = ModList.get().getModContainerById(HologramPanel.MOD_ID).getOrNull()!!
            val configName = Config.modFolderConfig("plugins/${plugin.location()}").replace(":", "_")
            val configSpec = configBuilder.build()
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, configSpec, configName)
            val globalSetting = PluginGlobalSetting(plugin, pluginEnable, providerEnableData)
            globalPluginSettings[plugin] = globalSetting
            globalPluginSettingsMap[configSpec] = globalSetting
        }
    }

    class PluginGlobalSetting(
        val plugin: IHologramPlugin,
        val enable: ForgeConfigSpec.BooleanValue,
        val providerEnableData: Map<ComponentProvider<*, *>, ForgeConfigSpec.BooleanValue>
    )

    fun onPluginSettingChange(config: ModConfig) {
        globalPluginSettingsMap[config.getSpec()] ?: return
        //clear and re-collect providers
        ProviderManager.collectProvidersFromRegistry()
        //clear all cache
        this.block.clear()
        this.entity.clear()
        this.hideBlocks.clear()
        this.hideEntityTypes.clear()
        this.hideEntityCallback.clear()
        //re-collect by condition
        globalPluginSettings.forEach { (plugin, setting) ->
            if (!setting.enable.get()) return@forEach
            setting.providerEnableData.forEach { (provider, enable) ->
                if (!enable.get()) {
                    ProviderManager.removeProvider(provider)
                }
            }
            val registration = clientRegistration[plugin]!!
            block.addAll(registration.blockPopup)
            entity.addAll(registration.entityPopup)
            hideBlocks.addAll(registration.hideBlocks)
            hideEntityTypes.addAll(registration.hideEntityTypes)
            hideEntityCallback.addAll(registration.hideEntityCallback)
        }
        //invalidate cache
        ProviderManager.invalidateCache()
        //remove all, some provider may have disappeared
        HologramManager.clearAllHologram()
    }

    fun onClientRegisterEnd() {
        this.block.addAll(flatten(clientRegistration, HologramClientRegistration::blockPopup))
        this.entity.addAll(flatten(clientRegistration, HologramClientRegistration::entityPopup))
        this.hideBlocks.addAll(flatten(clientRegistration, HologramClientRegistration::hideBlocks))
        this.hideEntityTypes.addAll(flatten(clientRegistration, HologramClientRegistration::hideEntityTypes))
        this.hideEntityCallback.addAll(flatten(clientRegistration, HologramClientRegistration::hideEntityCallback))
    }

    fun hideBlock(block: Block) = this.hideBlocks.contains(block) || Config.Client.hideBlocksList.contains(block)
    fun hideEntity(entity: Entity) =
        this.hideEntityTypes.contains(entity.type) || this.hideEntityCallback.any { it.test(entity) } ||
                Config.Client.hideEntityTypesList.contains(entity.type)

    fun popUpBlock(pos: BlockPos, level: Level): List<HologramTicket<BlockHologramContext>> {
        val block = level.getBlockState(pos)
        if (hideBlock(block.block)) return emptyList()
        if (this.block.isEmpty() && Config.Client.popupAllNearByBlock.get()) {
            if (level.getBlockState(pos).hasBlockEntity()) {
                return listOf(HologramTicket.byPopUpDistance())
            }
        }
        for (popupCallback in this.block) {
            val list = popupCallback.popup(pos, block, level)
            if (list.isNotEmpty()) return list
        }
        return listOf()
    }

    fun popUpEntity(entity: Entity): List<HologramTicket<EntityHologramContext>> {
        if (hideEntity(entity)) return emptyList()
        if (this.entity.isEmpty() && Config.Client.popupAllNearbyEntity.get()) {
            return listOf(HologramTicket.byPopUpDistance())
        }
        for (popupCallback in this.entity) {
            val list = popupCallback.popup(entity)
            if (list.isNotEmpty()) return list
        }
        return listOf()
    }
}
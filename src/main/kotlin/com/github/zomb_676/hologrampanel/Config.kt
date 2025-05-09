package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.compat.ModInstalled
import com.github.zomb_676.hologrampanel.util.SearchBackend
import com.github.zomb_676.hologrampanel.util.SwitchMode
import com.github.zomb_676.hologrampanel.util.TooltipType
import com.github.zomb_676.hologrampanel.util.setAndSave
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.fml.javafmlmod.FMLModContainer
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.ModConfigSpec
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.TagsUpdatedEvent
import kotlin.jvm.optionals.getOrNull
import kotlin.streams.asSequence

object Config {
    fun registerConfig(container: FMLModContainer, modBus: IEventBus) {
        ModConfigSpec.Builder()
        container.registerConfig(ModConfig.Type.SERVER, Server.space, modFolderConfig("server"))
        container.registerConfig(ModConfig.Type.CLIENT, Client.space, modFolderConfig("client"))
        container.registerConfig(ModConfig.Type.CLIENT, Style.space, modFolderConfig("style"))

        container.registerExtensionPoint(IConfigScreenFactory::class.java, object : IConfigScreenFactory {
            override fun createScreen(container: ModContainer, modListScreen: Screen): Screen =
                ConfigurationScreen(container, modListScreen)
        })

        NeoForge.EVENT_BUS.addListener(::onTagUpdate)
    }

    fun onTagUpdate(event: TagsUpdatedEvent) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            Client.onTagUpdate(event.lookupProvider)
        }
    }

    fun modFolderConfig(fileName: String): String = "HologramPanel/$fileName.toml"

    fun initEvents(dist: Dist, modBus: IEventBus) {
        modBus.addListener(::onLoad)
        modBus.addListener(::onReload)
    }

    private fun onLoad(event: ModConfigEvent.Loading) {
        if (event.config.spec == Client.space) {
            Client.tryValidate()
        }
        PluginManager.getInstance().onPluginSettingChange(event.config)
    }

    private fun onReload(event: ModConfigEvent.Reloading) {
        if (event.config.spec == Client.space) {
            Client.tryValidate()
        }
        PluginManager.getInstance().onPluginSettingChange(event.config)
    }

    object Server {
        private val builder = ModConfigSpec.Builder()

        val updateInternal: ModConfigSpec.IntValue = builder
            .defineInRange("server_update_internal", 5, 1, 20)

        val updateAtUnloaded: ModConfigSpec.BooleanValue = builder
            .define("update_at_unloaded", false)

        val allowHologramInteractive: ModConfigSpec.BooleanValue = builder
            .define("allow_hologram_interactive", true)

        val syncRadius: ModConfigSpec.IntValue = builder
            .defineInRange("sync_radius", 10, 2, 48)

        val space: ModConfigSpec = builder.build()
    }

    object Client {
        private val builder = ModConfigSpec.Builder()

        val dropNonApplicableWidget: ModConfigSpec.BooleanValue = builder
            .define("drop_none_applicable_widget", true)

        val enablePopUp: ModConfigSpec.BooleanValue = builder
            .define("enable_pop_up", true)

        val popUpInterval: ModConfigSpec.IntValue = builder
            .defineInRange("popup_interval", 5, 1, 200)

        val popUpDistance: ModConfigSpec.IntValue = builder
            .defineInRange("popup_distance", 16, 4, 48)

        val popupAllNearbyEntity: ModConfigSpec.BooleanValue = builder
            .define("popup_all_nearby_entity", true)

        val popupAllNearByBlock: ModConfigSpec.BooleanValue = builder
            .define("popup_all_nearby_block", true)

        val transformerContextAfterMobConversation: ModConfigSpec.BooleanValue = builder
            .define("transform_context_after_mob_conversation", true)

        val renderMaxDistance: ModConfigSpec.DoubleValue = builder
            .defineInRange("render_max_distance", 8.0, 0.1, 16.0)

        val renderMinDistance: ModConfigSpec.DoubleValue = builder
            .defineInRange("render_min_distance", 1.0, 0.1, 16.0)

        val displayAfterNotSeen: ModConfigSpec.IntValue = builder
            .comment("measured in tick")
            .defineInRange("display_after_not_seen", 80, 1, 1200)

        val globalHologramScale: ModConfigSpec.DoubleValue = builder
            .defineInRange("global_hologram_range", 1.0, 0.1, 2.5)

        val skipHologramIfEmpty: ModConfigSpec.BooleanValue = builder
            .define("skip_hologram_if_empty", true)

        val displayInteractiveHint: ModConfigSpec.BooleanValue = builder
            .define("display_interactive_hint", true)

        val renderDebugLayer: ModConfigSpec.BooleanValue = builder
            .define("render_debug_layer", false)

        val renderDebugHologramLifeCycleBox: ModConfigSpec.BooleanValue = builder
            .define("render_debug_hologram_life_cycle_box", false)

        val renderWidgetDebugInfo: ModConfigSpec.BooleanValue = builder
            .define("render_widget_debug_info", false)

        val renderNetworkDebugInfo: ModConfigSpec.BooleanValue = builder
            .define("render_network_debug_info", false)

        val renderDebugTransientTarget: ModConfigSpec.BooleanValue = builder
            .define("render_debug_transient_target", false)

        val renderInteractTransientReMappingIndicator: ModConfigSpec.BooleanValue = builder
            .define("render_interact_transient_re_mapping_indicator", false)

        val searchBackend: ModConfigSpec.EnumValue<SearchBackend.Type> = builder
            .defineEnum("search_backend", SearchBackend.Type.AUTO)

        val forceDisplayModeSwitchType: ModConfigSpec.EnumValue<SwitchMode> = builder
            .defineEnum("force_display_mode_switch_type", SwitchMode.BY_PRESS)

        val tooltipLimitHeight: ModConfigSpec.IntValue = builder
            .comment("0 for no limit")
            .defineInRange("tooltip_limit_height", 50, 0, Int.MAX_VALUE)

        val pinScreenDistanceFactor: ModConfigSpec.DoubleValue = builder
            .defineInRange("pin_screen_distance_factor", 5.0, 1.0, 100.0)

        val hideEntityTypes: ModConfigSpec.ConfigValue<MutableList<String>> = builder
            .define("hide_entity_types", mutableListOf())

        val hideBlocks: ModConfigSpec.ConfigValue<MutableList<String>> = builder
            .define("hide_blocks", mutableListOf())

        internal val hideEntityTypesList: MutableSet<EntityType<*>> = mutableSetOf()
        internal val hideBlocksList: MutableSet<Block> = mutableSetOf()

        fun onTagUpdate(provider: HolderLookup.Provider) {
            run {
                val entityTypeData = provider.lookup(Registries.ENTITY_TYPE).getOrNull() ?: return@run
                val tags = entityTypeData.listTags().asSequence()
                    .associateBy { set -> set.key().location }
                for (typeString in this.hideEntityTypes.get()) {
                    val location = ResourceLocation.tryParse(typeString) ?: continue
                    if (tags.containsKey(location)) {
                        tags.getValue(location).forEach { type ->
                            hideEntityTypesList += type.value()
                        }
                        continue
                    }

                    val type = BuiltInRegistries.ENTITY_TYPE.getOptional(location).getOrNull()
                    if (type != null) {
                        hideEntityTypesList += type
                        continue
                    }

                    HologramPanel.LOGGER.error("EntityType(s) $type not found")
                }
            }
            run {
                val blockTypeData = provider.lookup(Registries.BLOCK).getOrNull() ?: return@run
                val tags = blockTypeData.listTags().asSequence()
                    .associateBy { set -> set.key().location }
                for (blockString in this.hideBlocks.get()) {
                    val location = ResourceLocation.tryParse(blockString) ?: continue
                    if (tags.containsKey(location)) {
                        tags.getValue(location).forEach { type ->
                            hideBlocksList += type.value()
                        }
                        continue
                    }

                    val type = BuiltInRegistries.BLOCK.getOptional(location).getOrNull()
                    if (type != null) {
                        hideBlocksList += type
                        continue
                    }

                    HologramPanel.LOGGER.error("Block(s) $type not found")
                }
            }
        }

        fun tryValidate() {
            if (renderMaxDistance.get() <= renderMinDistance.get()) {
                renderMinDistance.set(1.0)
                renderMinDistance.set(8.0)
            }
            when (searchBackend.get()) {
                SearchBackend.Type.REI -> if (ModInstalled.reiInstalled) return
                SearchBackend.Type.JEI -> if (ModInstalled.jeiInstalled) return
                else -> return
            }
            searchBackend.setAndSave(SearchBackend.Type.AUTO)
            Minecraft.getInstance().gui.chat.addMessage(
                Component.literal(
                    "search backend switch to auto for fallback"
                )
            )
            Minecraft.getInstance().level?.registryAccess()?.also { access ->
                onTagUpdate(access)
            }
        }

        val space: ModConfigSpec = builder.build()
    }

    object Style {
        private val builder = ModConfigSpec.Builder()

        val itemTooltipType: ModConfigSpec.EnumValue<TooltipType> = builder
            .defineEnum("item_tooltip_type", TooltipType.SCREEN_SMART_BACKGROUND)

        val renderInteractIndicator: ModConfigSpec.BooleanValue = builder
            .define("render_interact_indicator", true)

        val interactIndicatorDistance: ModConfigSpec.IntValue = builder
            .defineInRange("interact_indicator_distance", 8, 1, 20)

        val interactIndicatorPercent: ModConfigSpec.DoubleValue = builder
            .defineInRange("interact_indicator_percent", 0.2, 0.001, 0.999)

        val renderSelectedIndicator: ModConfigSpec.BooleanValue = builder
            .define("render_selected_indicator", true)

        val selectedIndicatorDistance: ModConfigSpec.IntValue = builder
            .defineInRange("selected_indicator_distance", 12, 1, 20)

        val selectedIndicatorPercent: ModConfigSpec.DoubleValue = builder
            .defineInRange("selected_indicator_percent", 0.2, 0.001, 0.999)

        val widgetBackgroundAlpha: ModConfigSpec.IntValue = builder
            .defineInRange("widget_background_alpha", 0x7f, 0x00, 0xff)

        val pinPaddingLeft: ModConfigSpec.IntValue = builder
            .defineInRange("pin_padding_left", 0, 10, Int.MAX_VALUE)

        val pinPaddingUp: ModConfigSpec.IntValue = builder
            .defineInRange("pin_padding_up", 0, 10, Int.MAX_VALUE)

        val pinPromptLineWidth: ModConfigSpec.DoubleValue = builder
            .defineInRange("pin_prompt_line_width", 0.8, 0.001, 10.0)

        val pinPromptRadius: ModConfigSpec.DoubleValue = builder
            .defineInRange("pin_prompt_radius", 10.0, 1.0, 100.0)

        val pinPromptTerminalStraightLineLength: ModConfigSpec.DoubleValue = builder
            .defineInRange("pin_prompt_terminal_straight_line_length", 20.0, 0.1, 100.0)

        val dragPromptXOffset: ModConfigSpec.DoubleValue = builder
            .defineInRange("drag_prompt_x_offset", 3.0, -1000.0, 1000.0)

        val dragPromptYOffset: ModConfigSpec.DoubleValue = builder
            .defineInRange("drag_prompt_y_offset", 3.0, -1000.0, 1000.0)

        val dragPromptAlpha: ModConfigSpec.DoubleValue = builder
            .defineInRange("drag_prompt_alpha", 0.8, 0.01, 1.0)

        val space: ModConfigSpec = builder.build()
    }

}
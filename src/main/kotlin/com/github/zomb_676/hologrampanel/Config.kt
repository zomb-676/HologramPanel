package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.util.SearchBackend
import com.github.zomb_676.hologrampanel.util.TooltipType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.ConfigScreenHandler
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.config.ModConfigEvent

object Config {
    fun registerConfig(modBus: IEventBus) {
        val context = ModLoadingContext.get()
        context.registerConfig(ModConfig.Type.SERVER, Server.space, modFolderConfig("server"))
        context.registerConfig(ModConfig.Type.CLIENT, Client.space, modFolderConfig("client"))
        context.registerConfig(ModConfig.Type.CLIENT, Style.space, modFolderConfig("style"))

        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory::class.java,) {
            ConfigScreenHandler.ConfigScreenFactory { mc, screen -> screen }
        }
    }

    fun modFolderConfig(fileName: String): String = "HologramPanel/$fileName.toml"

    fun initEvents(dist: Dist, modBus: IEventBus) {
        modBus.addListener(::onLoad)
        modBus.addListener(::onReload)
    }

    private fun onLoad(event: ModConfigEvent.Loading) {
        if (event.config.getSpec() == Client.space) {
            Client.tryValidate()
        }
        PluginManager.getInstance().onPluginSettingChange(event.config)
    }

    private fun onReload(event: ModConfigEvent.Reloading) {
        if (event.config.getSpec() == Client.space) {
            Client.tryValidate()
        }
        PluginManager.getInstance().onPluginSettingChange(event.config)
    }

    object Server {
        private val builder = ForgeConfigSpec.Builder()

        val updateInternal: ForgeConfigSpec.IntValue = builder
            .defineInRange("server_update_internal", 5, 1, 20)

        val updateAtUnloaded: ForgeConfigSpec.BooleanValue = builder
            .define("update_at_unloaded", false)

        val allowHologramInteractive: ForgeConfigSpec.BooleanValue = builder
            .define("allow_hologram_interactive", true)

        val syncRadius: ForgeConfigSpec.IntValue = builder
            .defineInRange("sync_radius", 10, 2, 48)

        val space: ForgeConfigSpec = builder.build()
    }

    object Client {
        private val builder = ForgeConfigSpec.Builder()

        val dropNonApplicableWidget: ForgeConfigSpec.BooleanValue = builder
            .define("drop_none_applicable_widget", true)

        val enablePopUp: ForgeConfigSpec.BooleanValue = builder
            .define("enable_pop_up", true)

        val popUpInterval: ForgeConfigSpec.IntValue = builder
            .defineInRange("popup_interval", 5, 1, 200)

        val popUpDistance: ForgeConfigSpec.IntValue = builder
            .defineInRange("popup_distance", 16, 4, 48)

        val popupAllNearbyEntity: ForgeConfigSpec.BooleanValue = builder
            .define("popup_all_nearby_entity", true)

        val popupAllNearByBlock: ForgeConfigSpec.BooleanValue = builder
            .define("popup_all_nearby_block", true)

        val transformerContextAfterMobConversation: ForgeConfigSpec.BooleanValue = builder
            .define("transform_context_after_mob_conversation", true)

        val renderMaxDistance: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("render_max_distance", 8.0, 0.1, 16.0)

        val renderMinDistance: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("render_min_distance", 1.0, 0.1, 16.0)

        val displayAfterNotSeen: ForgeConfigSpec.IntValue = builder
            .comment("measured in tick")
            .defineInRange("display_after_not_seen", 80, 1, 1200)

        val globalHologramScale: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("global_hologram_range", 1.0, 0.1, 2.5)

        val skipHologramIfEmpty: ForgeConfigSpec.BooleanValue = builder
            .define("skip_hologram_if_empty", true)

        val displayInteractiveHint: ForgeConfigSpec.BooleanValue = builder
            .define("display_interactive_hint", true)

        val renderDebugLayer: ForgeConfigSpec.BooleanValue = builder
            .define("render_debug_layer", false)

        val renderDebugHologramLifeCycleBox: ForgeConfigSpec.BooleanValue = builder
            .define("render_debug_hologram_life_cycle_box", false)

        val renderWidgetDebugInfo: ForgeConfigSpec.BooleanValue = builder
            .define("render_widget_debug_info", false)

        val renderNetworkDebugInfo: ForgeConfigSpec.BooleanValue = builder
            .define("render_network_debug_info", false)

        val renderDebugTransientTarget: ForgeConfigSpec.BooleanValue = builder
            .define("render_debug_transient_target", false)

        val renderInteractTransientReMappingIndicator: ForgeConfigSpec.BooleanValue = builder
            .define("render_interact_transient_re_mapping_indicator", false)

        val searchBackend: ForgeConfigSpec.EnumValue<SearchBackend.Type> = builder
            .defineEnum("search_backend", SearchBackend.Type.AUTO)

        fun tryValidate() {
            if (renderMaxDistance.get() <= renderMinDistance.get()) {
                renderMinDistance.set(1.0)
                renderMinDistance.set(8.0)
            }
        }

        val space: ForgeConfigSpec = builder.build()
    }

    object Style {
        private val builder = ForgeConfigSpec.Builder()

        val itemTooltipType: ForgeConfigSpec.EnumValue<TooltipType> = builder
            .defineEnum("item_tooltip_type", TooltipType.SCREEN_NO_BACKGROUND)

        val renderInteractIndicator: ForgeConfigSpec.BooleanValue = builder
            .define("render_interact_indicator", true)

        val interactIndicatorDistance: ForgeConfigSpec.IntValue = builder
            .defineInRange("interact_indicator_distance", 8, 1, 20)

        val interactIndicatorPercent: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("interact_indicator_percent", 0.2, 0.001, 0.999)

        val widgetBackgroundAlpha: ForgeConfigSpec.IntValue = builder
            .defineInRange("widget_background_alpha", 0x7f, 0x00, 0xff)

        val space: ForgeConfigSpec = builder.build()
    }

}
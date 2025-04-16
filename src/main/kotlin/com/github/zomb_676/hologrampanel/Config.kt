package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.util.SearchBackend
import com.github.zomb_676.hologrampanel.util.TooltipType
import net.minecraft.client.gui.screens.Screen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.fml.javafmlmod.FMLModContainer
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.ModConfigSpec

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

        val globalHologramScale: ModConfigSpec.DoubleValue = builder
            .defineInRange("global_hologram_range", 1.0, 0.01, 20.0)

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

        val searchBackend: ModConfigSpec.EnumValue<SearchBackend.Type> = builder
            .defineEnum("search_backend", SearchBackend.Type.AUTO)

        fun tryValidate() {
            if (renderMaxDistance.get() <= renderMinDistance.get()) {
                renderMinDistance.set(1.0)
                renderMinDistance.set(8.0)
            }
        }

        val space: ModConfigSpec = builder.build()
    }

    object Style {
        private val builder = ModConfigSpec.Builder()

        val itemTooltipType: ModConfigSpec.EnumValue<TooltipType> = builder
            .defineEnum("item_tooltip_type", TooltipType.SCREEN_SMART_BACKGROUND)

        val renderLookIndicator: ModConfigSpec.BooleanValue = builder
            .define("render_look_indicator", true)

        val lookIndicatorDistance: ModConfigSpec.IntValue = builder
            .defineInRange("look_indicator_distance", 8, 1, 20)

        val lookIndicatorPercent: ModConfigSpec.DoubleValue = builder
            .defineInRange("look_indicator_percent", 0.2, 0.001, 0.999)

        val widgetBackgroundAlpha: ModConfigSpec.IntValue = builder
            .defineInRange("widget_background_alpha", 0x7f, 0x00, 0xff)

        val space: ModConfigSpec = builder.build()
    }

}
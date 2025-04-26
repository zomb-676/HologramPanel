package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.compat.ModInstalled
import com.github.zomb_676.hologrampanel.util.SearchBackend
import com.github.zomb_676.hologrampanel.util.SwitchMode
import com.github.zomb_676.hologrampanel.util.TooltipType
import com.github.zomb_676.hologrampanel.util.setAndSave
import net.minecraft.client.Minecraft
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TagsUpdatedEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.config.ModConfigEvent
import net.minecraftforge.fml.loading.FMLLoader
import kotlin.jvm.optionals.getOrNull
import kotlin.streams.asSequence

object Config {
    fun registerConfig(modBus: IEventBus) {
        val context = ModLoadingContext.get()
        context.registerConfig(ModConfig.Type.SERVER, Server.space, modFolderConfig("server"))
        context.registerConfig(ModConfig.Type.CLIENT, Client.space, modFolderConfig("client"))
        context.registerConfig(ModConfig.Type.CLIENT, Style.space, modFolderConfig("style"))

        MinecraftForge.EVENT_BUS.addListener(::onTagUpdate)
    }

    fun onTagUpdate(event: TagsUpdatedEvent) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            Client.onTagUpdate(event.registryAccess)
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

        val forceDisplayModeSwitchType: ForgeConfigSpec.EnumValue<SwitchMode> = builder
            .defineEnum("force_display_mode_switch_type", SwitchMode.BY_PRESS)

        val tooltipLimitHeight: ForgeConfigSpec.IntValue = builder
            .comment("0 for no limit")
            .defineInRange("tooltip_limit_height", 50, 0, Int.MAX_VALUE)

        val pinScreenDistanceFactor: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("pin_screen_distance_factor", 5.0, 1.0, 100.0)

        val hideEntityTypes: ForgeConfigSpec.ConfigValue<MutableList<String>> = builder
            .define("hide_entity_types", mutableListOf())

        val hideBlocks: ForgeConfigSpec.ConfigValue<MutableList<String>> = builder
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

        val renderSelectedIndicator: ForgeConfigSpec.BooleanValue = builder
            .define("render_selected_indicator", true)

        val selectedIndicatorDistance: ForgeConfigSpec.IntValue = builder
            .defineInRange("selected_indicator_distance", 12, 1, 20)

        val selectedIndicatorPercent: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("selected_indicator_percent", 0.2, 0.001, 0.999)

        val widgetBackgroundAlpha: ForgeConfigSpec.IntValue = builder
            .defineInRange("widget_background_alpha", 0x7f, 0x00, 0xff)

        val pinPaddingLeft: ForgeConfigSpec.IntValue = builder
            .defineInRange("pin_padding_left", 0, 10, Int.MAX_VALUE)

        val pinPaddingUp: ForgeConfigSpec.IntValue = builder
            .defineInRange("pin_padding_up", 0, 10, Int.MAX_VALUE)

        val pinPromptLineWidth: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("pin_prompt_line_width", 0.8, 0.001, 10.0)

        val pinPromptRadius: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("pin_prompt_radius", 10.0, 1.0, 100.0)

        val pinPromptTerminalStraightLineLength: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("pin_prompt_terminal_straight_line_length", 20.0, 0.1, 100.0)

        val dragPromptXOffset: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("drag_prompt_x_offset", 3.0, -1000.0, 1000.0)

        val dragPromptYOffset: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("drag_prompt_y_offset", 3.0, -1000.0, 1000.0)

        val dragPromptAlpha: ForgeConfigSpec.DoubleValue = builder
            .defineInRange("drag_prompt_alpha", 0.8, 0.01, 1.0)

        val space: ForgeConfigSpec = builder.build()
    }

}
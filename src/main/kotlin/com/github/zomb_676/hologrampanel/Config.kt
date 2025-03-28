package com.github.zomb_676.hologrampanel

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
        container.registerConfig(ModConfig.Type.SERVER, Server.space)
        container.registerConfig(ModConfig.Type.CLIENT, Client.space)

        container.registerExtensionPoint(IConfigScreenFactory::class.java, object : IConfigScreenFactory {
            override fun createScreen(container: ModContainer, modListScreen: Screen): Screen =
                ConfigurationScreen(container, modListScreen)
        })
    }

    fun initEvents(dist: Dist, modBus: IEventBus) {
        modBus.addListener(::onLoad)
        modBus.addListener(::onReload)
    }

    private fun onLoad(event: ModConfigEvent.Loading) {
        if (event.config.spec == Client.space) {
            Client.tryValidate()
        }
    }

    private fun onReload(event: ModConfigEvent.Reloading) {
        if (event.config.spec == Client.space) {
            Client.tryValidate()
        }
    }

    object Server {
        private val builder = ModConfigSpec.Builder()

        val updateInternal: ModConfigSpec.IntValue = builder
            .defineInRange("server_update_internal", 5, 1, 20)

        val updateAtUnloaded: ModConfigSpec.BooleanValue = builder
            .define("update_at_unloaded", false)

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
            .defineInRange("popup_distance", 32, 4, 48)

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

        val skipHologramIfEmpty: ModConfigSpec.BooleanValue = builder
            .define("skip_hologram_if_empty", true)

        val renderDebugLayer : ModConfigSpec.BooleanValue = builder
            .define("render_debug_value", false)

        val renderDebugBox: ModConfigSpec.BooleanValue = builder
            .define("render_debug_box", true)

        fun tryValidate() {
            if (renderMaxDistance.get() <= renderMinDistance.get()) {
                renderMinDistance.set(1.0)
                renderMinDistance.set(8.0)
            }
        }

        val space: ModConfigSpec = builder.build()
    }


}
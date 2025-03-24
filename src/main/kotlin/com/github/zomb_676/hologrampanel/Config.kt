package com.github.zomb_676.hologrampanel

import net.minecraft.client.gui.screens.Screen
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.javafmlmod.FMLModContainer
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.ModConfigSpec

object Config {
    fun registerConfig(container: FMLModContainer, modBus: IEventBus) {
        ModConfigSpec.Builder()
        container.registerConfig(ModConfig.Type.SERVER, Server.builder.build())
        container.registerConfig(ModConfig.Type.CLIENT, Client.builder.build())

        container.registerExtensionPoint(IConfigScreenFactory::class.java, object : IConfigScreenFactory {
            override fun createScreen(container: ModContainer, modListScreen: Screen): Screen =
                ConfigurationScreen(container, modListScreen)
        })
    }


    object Server {
        internal val builder = ModConfigSpec.Builder()

        val updateInternal: ModConfigSpec.IntValue = builder
            .defineInRange("update_internal", 5, 1, 20)

    }

    object Client {
        internal val builder = ModConfigSpec.Builder()

        val dropNonApplicableWidget: ModConfigSpec.BooleanValue = builder
            .define("drop_none_applicable_widget", true)

        val transformerContextAfterMobConversation: ModConfigSpec.BooleanValue = builder
            .define("transformer_context_after_mob_conversation", true)
    }


}
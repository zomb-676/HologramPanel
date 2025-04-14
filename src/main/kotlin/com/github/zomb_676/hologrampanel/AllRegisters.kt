package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.mojang.blaze3d.platform.InputConstants
import io.netty.buffer.ByteBuf
import net.minecraft.client.KeyMapping
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.fluids.FluidType
import net.neoforged.neoforge.registries.NeoForgeRegistries
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder
import org.lwjgl.glfw.GLFW

object AllRegisters {
    fun initEvents(dist: Dist, modBus: IEventBus) {
        modBus.addListener(::addNewRegistry)
    }

    private fun addNewRegistry(event: NewRegistryEvent) {
        event.register(ComponentHologramProviderRegistry.REGISTRY)
    }

    object ComponentHologramProviderRegistry {
        val RESOURCE_KEY: ResourceKey<Registry<ComponentProvider<*, *>>> = ResourceKey
            .createRegistryKey(HologramPanel.rl("component_hologram_provider"))
        val REGISTRY: Registry<ComponentProvider<*, *>> =
            RegistryBuilder(RESOURCE_KEY)
                .sync(true)
                .create()
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentProvider<*, *>> =
            ByteBufCodecs.registry(RESOURCE_KEY)

        fun getId(provider: ComponentProvider<*, *>) = REGISTRY.getId(provider)
        fun byId(id: Int) = REGISTRY.byId(id)
    }

    object Codecs {
        val LEVEL_STREAM_CODE: StreamCodec<ByteBuf, ResourceKey<Level>> =
            ResourceKey.streamCodec(Registries.DIMENSION)

        val FLUID_TYPE_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidType> =
            ByteBufCodecs.registry(NeoForgeRegistries.Keys.FLUID_TYPES)
    }

    object KeyMapping {
        const val KEY_CATEGORY = "key.categories.${HologramPanel.MOD_ID}"

        fun register(event : RegisterKeyMappingsEvent) {
            event.register(panelKey)
            event.register(scaleKey)
            event.register(collapseKey)
            event.register(pingScreenKey)
        }

        val panelKey = KeyMapping(
            "key.${HologramPanel.MOD_ID}.selector_panel_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            KEY_CATEGORY
        )

        val scaleKey = KeyMapping(
            "key.${HologramPanel.MOD_ID}.scale_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            KEY_CATEGORY
        )

        val collapseKey = KeyMapping(
            "key.${HologramPanel.MOD_ID}.collapse_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            KEY_CATEGORY
        )

        val pingScreenKey = KeyMapping(
            "key.${HologramPanel.MOD_ID}.ping_screen_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            KEY_CATEGORY
        )

    }
}
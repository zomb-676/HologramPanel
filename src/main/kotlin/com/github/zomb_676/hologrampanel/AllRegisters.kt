package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.interactive.HologramInteractiveTarget
import net.minecraft.core.Registry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder

object AllRegisters {
    fun initEvents(dist: Dist, modBus: IEventBus) {
        modBus.addListener(::addNewRegistry)
        BuildInInteractiveHologram.REGISTRY.register(modBus)
    }

    private fun addNewRegistry(event: NewRegistryEvent) {
        event.register(InteractiveHologramRegistry.INTERACTIVE_HOLOGRAM_REGISTRY)
        event.register(ComponentHologramProviderRegistry.REGISTRY)
    }

    object InteractiveHologramRegistry {
        val INTERACTIVE_HOLOGRAM_KEY: ResourceKey<Registry<HologramInteractiveTarget.Provider<*>>> = ResourceKey
            .createRegistryKey(HologramPanel.rl("interactive_hologram"))
        val INTERACTIVE_HOLOGRAM_REGISTRY: Registry<HologramInteractiveTarget.Provider<*>> =
            RegistryBuilder(INTERACTIVE_HOLOGRAM_KEY)
                .sync(true)
                .create()
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, HologramInteractiveTarget.Provider<*>> =
            ByteBufCodecs.registry(INTERACTIVE_HOLOGRAM_KEY)
    }

    object BuildInInteractiveHologram {
        internal val REGISTRY: DeferredRegister<HologramInteractiveTarget.Provider<*>> = DeferredRegister.create(
            InteractiveHologramRegistry.INTERACTIVE_HOLOGRAM_REGISTRY,
            HologramPanel.MOD_ID
        )

        val furnace = REGISTRY.register("furnace") { rl ->
            HologramInteractiveTarget.Companion.Furnace
        }
    }

    object ComponentHologramProviderRegistry {
        val RESOURCE_KEY: ResourceKey<Registry<ComponentProvider<*>>> = ResourceKey
            .createRegistryKey(HologramPanel.rl("component_hologram_provider"))
        val REGISTRY: Registry<ComponentProvider<*>> =
            RegistryBuilder(RESOURCE_KEY)
                .sync(true)
                .create()
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentProvider<*>> =
            ByteBufCodecs.registry(RESOURCE_KEY)
    }
}
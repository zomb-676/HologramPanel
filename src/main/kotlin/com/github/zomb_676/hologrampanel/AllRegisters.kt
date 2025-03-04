package com.github.zomb_676.hologrampanel

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
    }

    object InteractiveHologramRegistry {
        val INTERACTIVE_HOLOGRAM_KEY: ResourceKey<Registry<HologramInteractiveTarget.Provider<*>>> = ResourceKey
            .createRegistryKey(HologramPanel.rl("interactive_hologram"))
        val INTERACTIVE_HOLOGRAM_REGISTRY: Registry<HologramInteractiveTarget.Provider<*>> =
            RegistryBuilder(INTERACTIVE_HOLOGRAM_KEY)
                .sync(true)
                .create()
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, HologramInteractiveTarget.Provider<*>> =
            ByteBufCodecs.registry(INTERACTIVE_HOLOGRAM_KEY)
    }

    object BuildInInteractiveHologram {
        internal val REGISTRY = DeferredRegister.create(
            InteractiveHologramRegistry.INTERACTIVE_HOLOGRAM_REGISTRY,
            HologramPanel.MOD_ID
        )

        val furnace = REGISTRY.register("furnace") { rl ->
            HologramInteractiveTarget.Companion.Furnace
        }
    }
}
package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import io.netty.buffer.ByteBuf
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.fluids.FluidType
import net.neoforged.neoforge.registries.NeoForgeRegistries
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder

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
    }

    object Codecs {
        val LEVEL_STREAM_CODE: StreamCodec<ByteBuf, ResourceKey<Level>> =
            ResourceKey.streamCodec(Registries.DIMENSION)

        val FLUID_TYPE_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidType> =
            ByteBufCodecs.registry(NeoForgeRegistries.Keys.FLUID_TYPES)
    }
}
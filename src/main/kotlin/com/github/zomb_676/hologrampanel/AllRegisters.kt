package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.IContextType
import com.github.zomb_676.hologrampanel.api.IServerDataProcessor
import com.github.zomb_676.hologrampanel.widget.interactive.HologramInteractiveTarget
import net.minecraft.core.BlockPos
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
        BuildInContextType.REGISTRY.register(modBus)
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
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, HologramInteractiveTarget.Provider<*>> =
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

    object IServerDataProcessorRegistry {
        val SERVER_DATA_PROCESSOR_KEY: ResourceKey<Registry<IServerDataProcessor>> = ResourceKey
            .createRegistryKey(HologramPanel.rl("server_data_processor"))
        val SERVER_DATA_PROCESSOR_REGISTRY: Registry<IServerDataProcessor> =
            RegistryBuilder(SERVER_DATA_PROCESSOR_KEY)
                .sync(true)
                .create()
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IServerDataProcessor> =
            ByteBufCodecs.registry(SERVER_DATA_PROCESSOR_KEY)

        val LIST_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, List<IServerDataProcessor>> =
            object : StreamCodec<RegistryFriendlyByteBuf, List<IServerDataProcessor>> {
                override fun decode(buffer: RegistryFriendlyByteBuf): List<IServerDataProcessor> {
                    val count = buffer.readVarInt()
                    return List(count) {
                        STREAM_CODEC.decode(buffer)
                    }
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf,
                    value: List<IServerDataProcessor>
                ) {
                    buffer.writeVarInt(value.size)
                    value.forEach { STREAM_CODEC.encode(buffer, it) }
                }
            }
    }

    object ContextTypeRegistry {
        val CONTEXT_TYPE_KEY: ResourceKey<Registry<IContextType<*>>> = ResourceKey
            .createRegistryKey(HologramPanel.rl("context_type"))

        val SERVER_DATA_PROCESSOR_REGISTRY: Registry<IContextType<*>> =
            RegistryBuilder(CONTEXT_TYPE_KEY)
                .sync(true)
                .create()
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IContextType<*>> =
            ByteBufCodecs.registry(CONTEXT_TYPE_KEY)
    }

    object BuildInContextType {
        internal val REGISTRY = DeferredRegister.create(
            ContextTypeRegistry.SERVER_DATA_PROCESSOR_REGISTRY,
            HologramPanel.MOD_ID
        )

        val blockPos = REGISTRY.register("block_pos") { rl ->
            IContextType.fromStreamCodec(BlockPos.STREAM_CODEC)
        }
    }
}
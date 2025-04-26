package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import com.github.zomb_676.hologrampanel.projector.ProjectorBlock
import com.github.zomb_676.hologrampanel.projector.ProjectorBlockEntity
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.client.KeyMapping
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fluids.FluidType
import net.minecraftforge.registries.*
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import java.util.function.Supplier

object AllRegisters {
    fun initEvents(dist: Dist, modBus: IEventBus) {
        modBus.addListener(::addNewRegistry)
        Items.ITEMS.register(modBus)
        Blocks.BLOCKS.register(modBus)
        BlockEntities.BLOCK_ENTITIES.register(modBus)
        modBus.addListener(::addToCreativeTab)
    }

    private fun addToCreativeTab(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey != CreativeModeTabs.FUNCTIONAL_BLOCKS) return
        event.accept(Items.projectItem)
    }

    private fun addNewRegistry(event: NewRegistryEvent) {
        ComponentHologramProviderRegistry.registry = event.create(ComponentHologramProviderRegistry.registryBuilder)
    }

    @Suppress("UnstableApiUsage")
    object ComponentHologramProviderRegistry {
        val location = HologramPanel.rl("component_hologram_provider")
        val RESOURCE_KEY: ResourceKey<Registry<ComponentProvider<*, *>>> = ResourceKey
            .createRegistryKey(location)
        val registryBuilder: RegistryBuilder<ComponentProvider<*, *>> = RegistryBuilder<ComponentProvider<*, *>>()
            .setName(location)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentProvider<*, *>> =
            object : StreamCodec<RegistryFriendlyByteBuf, ComponentProvider<*, *>> {
                override fun decode(buffer: RegistryFriendlyByteBuf): ComponentProvider<*, *> {
                    return byId(buffer.readVarInt())
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf,
                    value: ComponentProvider<*, *>
                ) {
                    buffer.writeVarInt(getId(value))
                }

            }

        lateinit var registry: Supplier<IForgeRegistry<ComponentProvider<*, *>>>

        val ID_MAP by lazy { RegistryManager.ACTIVE.getRegistry(RESOURCE_KEY) }

        fun getId(provider: ComponentProvider<*, *>) = ID_MAP.getID(provider)
        fun byId(id: Int): ComponentProvider<*, *> = ID_MAP.getValue(id)

    }

    object Items {
        internal val ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HologramPanel.MOD_ID)

        val projectItem = ITEMS.register("projector") {
            BlockItem(Blocks.projector.get(), Item.Properties())
        }
    }

    object Blocks {
        internal val BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,HologramPanel.MOD_ID)

        val projector = BLOCKS.register("projector") {
            ProjectorBlock(BlockBehaviour.Properties.of())
        }
    }

    object BlockEntities {
        internal val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HologramPanel.MOD_ID)

        val projectorType: RegistryObject<BlockEntityType<ProjectorBlockEntity>> =
            BLOCK_ENTITIES.register("projector", Supplier {
                BlockEntityType(::ProjectorBlockEntity, setOf(Blocks.projector.get()), null)
            })
    }

    object StreamCodecs {
        val LEVEL_STREAM_CODE: StreamCodec<FriendlyByteBuf, ResourceKey<Level>> = object : StreamCodec<FriendlyByteBuf, ResourceKey<Level>> {
            override fun decode(buffer: FriendlyByteBuf): ResourceKey<Level> {
                return buffer.readResourceKey(Registries.DIMENSION)
            }

            override fun encode(buffer: FriendlyByteBuf, value: ResourceKey<Level>) {
                buffer.writeResourceKey(value)
            }
        }

        val FLUID_TYPE_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidType> = object : StreamCodec<RegistryFriendlyByteBuf, FluidType> {
            override fun decode(buffer: RegistryFriendlyByteBuf): FluidType {
                val key = buffer.readResourceKey(ForgeRegistries.Keys.FLUID_TYPES)
                return ForgeRegistries.FLUID_TYPES.get().getValue(key.location())!!
            }

            override fun encode(
                buffer: RegistryFriendlyByteBuf,
                value: FluidType
            ) {
                val resourceKey = ForgeRegistries.FLUID_TYPES.get().getResourceKey(value).orElseThrow()
                buffer.writeResourceKey(resourceKey)
            }

        }
    }

    object KeyMapping {
        const val KEY_CATEGORY = "key.categories.${HologramPanel.MOD_ID}"

        fun register(event: RegisterKeyMappingsEvent) {
            event.register(panelKey)
            event.register(scaleKey)
            event.register(collapseKey)
            event.register(freeMouseMoveKey)
            event.register(forceDisplayKey)
        }

        val panelKey = KeyMapping(
            "key.${HologramPanel.MOD_ID}.selector_panel_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
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

        val freeMouseMoveKey = KeyMapping(
            "key.${HologramPanel.MOD_ID}.free_mouse_move_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY
        )

        val forceDisplayKey = KeyMapping(
            "key.${HologramPanel.MOD_ID}.force_display_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            KEY_CATEGORY
        )
    }

    object Codecs {
        val VEC2F: Codec<Vector2f> = RecordCodecBuilder.create { ins ->
            ins.group(
                Codec.FLOAT.fieldOf("x").forGetter { it.x },
                Codec.FLOAT.fieldOf("y").forGetter { it.y },
            ).apply(ins, ::Vector2f)
        }
        val VEC3F: Codec<Vector3f> = RecordCodecBuilder.create { ins ->
            ins.group(
                Codec.FLOAT.fieldOf("x").forGetter { it.x },
                Codec.FLOAT.fieldOf("y").forGetter { it.y },
                Codec.FLOAT.fieldOf("z").forGetter { it.z },
            ).apply(ins, ::Vector3f)
        }
    }
}
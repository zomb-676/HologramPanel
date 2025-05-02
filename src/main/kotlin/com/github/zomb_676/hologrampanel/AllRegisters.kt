package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.projector.ProjectorBlock
import com.github.zomb_676.hologrampanel.projector.ProjectorBlockEntity
import com.google.common.base.Supplier
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.client.KeyMapping
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.*
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW

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
        event.insertAfter(
            ItemStack(net.minecraft.world.item.Items.ENCHANTING_TABLE),
            ItemStack(Items.projectItem.get()),
            CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
        )
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

    object Items {
        internal val ITEMS = DeferredRegister.createItems(HologramPanel.MOD_ID)

        val projectItem = ITEMS.registerSimpleBlockItem("projector", Blocks.projector)
    }

    object Blocks {
        internal val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(HologramPanel.MOD_ID)

        val projector: DeferredBlock<ProjectorBlock> = BLOCKS.registerBlock("projector", ::ProjectorBlock)
    }

    object BlockEntities {
        internal val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, HologramPanel.MOD_ID)

        val projectorType: DeferredHolder<BlockEntityType<*>, BlockEntityType<ProjectorBlockEntity>> =
            BLOCK_ENTITIES.register("projector", Supplier {
                BlockEntityType(::ProjectorBlockEntity, setOf(Blocks.projector.get()), null)
            })
    }

    object StreamCodecs {
        val LEVEL_STREAM_CODE: StreamCodec<ByteBuf, ResourceKey<Level>> =
            ResourceKey.streamCodec(Registries.DIMENSION)
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
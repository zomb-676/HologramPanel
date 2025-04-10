package com.github.zomb_676.hologrampanel.addon

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.*
import com.github.zomb_676.hologrampanel.addon.vanilla.*
import com.github.zomb_676.hologrampanel.api.*
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Block

@HologramPlugin
class BuildInPlugin : IHologramPlugin {
    override fun location(): ResourceLocation = HologramPanel.Companion.rl("build_in")

    override fun registerCommon(register: HologramCommonRegistration) {
        register.registerBlockComponent(FurnaceProvider)
        register.registerBlockComponent(BrewStandProvider)
        register.registerBlockComponent(CampfireProvider)
        register.registerBlockComponent(JukeBoxProvider)
        register.registerBlockComponent(EnderChestProvider)
        register.registerBlockComponent(LecternProvider)
        register.registerBlockComponent(CauldronBlockProvider)

        register.registerBlockComponent(UniversalContainerBlockProvider)
        register.registerBlockComponent(UniversalFluidBlockProvider)
        register.registerBlockComponent(UniversalEnergyBlockProvider)

        register.registerEntityComponent(EntityHealthProvider)
        register.registerEntityComponent(ItemEntityLifeSpanProvider)
        register.registerEntityComponent(ItemEntityTooltipProvider)
        register.registerEntityComponent(LivingEntityPotionProvider)
        register.registerEntityComponent(InventoryCarrierProvider)
        register.registerEntityComponent(UniversalContainerEntityProvider)
        register.registerEntityComponent(UniversalContainerItemProvider)
        register.registerEntityComponent(UniversalFluidEntityProvider)
        register.registerEntityComponent(UniversalFluidItemProvider)
        register.registerEntityComponent(UniversalEnergyEntityProvider)
        register.registerEntityComponent(UniversalEnergyItemProvider)
        register.registerEntityComponent(PlayerInventoryProvider)
    }

    override fun registerClient(register: HologramClientRegistration) {
    }

    companion object {
        data object DefaultBlockDescriptionProvider : ComponentProvider<BlockHologramContext, Block> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<BlockHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                builder.single("default_block") {
                    item("block_logo", context.getBlockState().block.asItem()).setScale(0.75)
                    component("block_name", context.getBlockState().block.name).setScale(1.5)
                }
            }

            override fun targetClass(): Class<Block> = Block::class.java

            override fun location(): ResourceLocation = HologramPanel.Companion.rl("default_block_description_provider")
        }

        data object DefaultEntityDescriptionProvider : ComponentProvider<EntityHologramContext, Entity> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<EntityHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                builder.single("default_entity") {
                    val entity = context.getEntity()
                    entity("entity", entity)
                    vertical("name_container") {
                        val typeName = entity.type.description
                        component("type_name", typeName)
                        if (entity.hasCustomName()) {
                            component("custom_name", entity.customName!!).setScale(0.8)
                        } else {
                            val name = entity.name
                            if (name !== typeName && name.string != typeName.string) {
                                component("name", name)
                            }
                        }
                    }
                }
            }

            override fun targetClass(): Class<Entity> = Entity::class.java

            override fun location(): ResourceLocation = HologramPanel.Companion.rl("default_entity_description_provider")
        }
    }
}
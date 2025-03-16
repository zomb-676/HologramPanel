package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.HologramClientRegistration
import com.github.zomb_676.hologrampanel.api.HologramCommonRegistration
import com.github.zomb_676.hologrampanel.api.HologramPlugin
import com.github.zomb_676.hologrampanel.api.IHologramPlugin
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import com.github.zomb_676.hologrampanel.widget.dynamic.IRenderElement
import io.netty.buffer.Unpooled
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.BeehiveBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.BrewingStandBlock
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.block.JukeboxBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BeehiveBlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.entity.CampfireBlockEntity
import net.minecraft.world.level.block.entity.JukeboxBlockEntity
import net.minecraft.world.level.material.Fluids
import kotlin.jvm.optionals.getOrNull

@HologramPlugin
class BuildInPlugin : IHologramPlugin {
    override fun location(): ResourceLocation = HologramPanel.Companion.rl("build_in")

    override fun registerCommon(register: HologramCommonRegistration) {
        register.registerBlockComponent(FurnaceProvider())
        register.registerBlockComponent(BrewStandProvider())
        register.registerBlockComponent(CampfireProvider())
        register.registerBlockComponent(JukeBoxProvider())
        register.registerBlockComponent(BeeHiveProvider())

        register.registerEntityComponent(EntityProvider())
        register.registerEntityComponent(ItemEntityLifeSpanProvider())
        register.registerEntityComponent(LivingEntityPotionProvider())
    }

    override fun registerClient(register: HologramClientRegistration) {
    }

    companion object {
        data object DefaultBlockDescriptionProvider : ComponentProvider<BlockHologramContext> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<BlockHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                builder.single("default_block") {
                    item(context.getBlockState().block.asItem()).setScale(0.75)
                    component(context.getBlockState().block.name).setScale(1.5)
                }
            }

            override fun targetClass(): Class<*> = Block::class.java

            override fun location(): ResourceLocation = HologramPanel.Companion.rl("default_block_description_provider")
        }

        data object DefaultEntityDescriptionProvider : ComponentProvider<EntityHologramContext> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<EntityHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                builder.single("default_entity") {
                    entity(context.getEntity())
                    component(context.getEntity().name)
                }
            }

            override fun targetClass(): Class<*> = Block::class.java

            override fun location(): ResourceLocation = HologramPanel.Companion.rl("default_block_description_provider")
        }
    }
}
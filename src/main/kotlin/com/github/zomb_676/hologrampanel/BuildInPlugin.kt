package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.HologramCommonRegistration
import com.github.zomb_676.hologrampanel.api.HologramPlugin
import com.github.zomb_676.hologrampanel.api.IHologramPlugin
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetBuilder
import com.github.zomb_676.hologrampanel.widget.component.ServerDataProvider
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity

@HologramPlugin
class BuildInPlugin : IHologramPlugin {
    override fun location(): ResourceLocation = HologramPanel.rl("build_in")

    override fun registerCommon(register: HologramCommonRegistration) {
        register.registerBlockComponent<Block>("block") { builder ->
            builder.single { component { getBlockState().block.name } }
        }
        register.registerBlockComponent(object : ComponentProvider<BlockHologramContext>,
            ServerDataProvider<BlockHologramContext> {
            override fun appendComponent(builder: HologramWidgetBuilder<BlockHologramContext>) {
                val context = builder.context
                val registryAccess = context.getPlayer().registryAccess()
                builder.single {
                    itemStack {
                        val data = context.attachedServerData()
                        if (data != null) {
                            ItemStack.parseOptional(registryAccess, data.getCompound("furnace_slot_0"))
                        } else {
                            ItemStack.EMPTY
                        }
                    }
                }
            }

            override fun targetClass(): Class<*> = AbstractFurnaceBlock::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("abstract_furnace_block")

            override fun appendServerData(
                additionData: CompoundTag,
                targetData: CompoundTag,
                context: BlockHologramContext
            ) {
                val furnace = context.getBlockEntity<AbstractFurnaceBlockEntity>() ?: return
                targetData.putIntArray(
                    "furnace_progress_data", intArrayOf(
                        furnace.litTimeRemaining,
                        furnace.litTotalTime,
                        furnace.cookingTimer,
                        furnace.cookingTotalTime
                    )
                )
                val items = furnace.items
                val registryAccess = context.getLevel().registryAccess()
                targetData.put("furnace_slot_0", items[0].saveOptional(registryAccess))
                targetData.put("furnace_slot_1", items[1].saveOptional(registryAccess))
                targetData.put("furnace_slot_2", items[2].saveOptional(registryAccess))
            }
        })
    }
}
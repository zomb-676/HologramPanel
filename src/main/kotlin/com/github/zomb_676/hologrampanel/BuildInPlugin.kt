package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.HologramCommonRegistration
import com.github.zomb_676.hologrampanel.api.HologramPlugin
import com.github.zomb_676.hologrampanel.api.IHologramPlugin
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import com.github.zomb_676.hologrampanel.widget.component.ServerDataProvider
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity

@HologramPlugin
class BuildInPlugin : IHologramPlugin {
    override fun location(): ResourceLocation = HologramPanel.rl("build_in")

    override fun registerCommon(register: HologramCommonRegistration) {
        register.registerBlockComponent(object : ServerDataProvider<BlockHologramContext> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<BlockHologramContext>,
                displayType: DisplayType
            ) {
                val remember = builder.context.getRememberData()
                val item0 by remember.serverItemStack(0, "furnace_slot_0")
                val item1 by remember.serverItemStack(1, "furnace_slot_1")
                val item2 by remember.serverItemStack(2, "furnace_slot_2")

                builder.single {
                    if (!item0.isEmpty) itemStack(item0)
                    if (!item1.isEmpty) itemStack(item1)
                    if (!item2.isEmpty) itemStack(item2)
                }
            }

            override fun targetClass(): Class<*> = AbstractFurnaceBlock::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("abstract_furnace_block")

            override fun appendServerData(
                additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
            ) {
                val furnace = context.getBlockEntity<AbstractFurnaceBlockEntity>() ?: return
                targetData.putIntArray(
                    "furnace_progress_data", intArrayOf(
                        furnace.litTimeRemaining, furnace.litTotalTime, furnace.cookingTimer, furnace.cookingTotalTime
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
package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import com.github.zomb_676.hologrampanel.widget.dynamic.IRenderElement
import com.github.zomb_676.hologrampanel.widget.dynamic.IRenderElement.ProgressData
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.material.Fluids

class FurnaceProvider : ServerDataProvider<BlockHologramContext>  {
    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val remember = builder.context.getRememberData()
        val item0 by remember.serverItemStack(0, "furnace_slot_0")
        val item1 by remember.serverItemStack(1, "furnace_slot_1")
        val item2 by remember.serverItemStack(2, "furnace_slot_2")
        val litTimeRemaining by remember.server(3, 0) { tag -> tag.getIntArray("furnace_progress_data")[0] }
        val litTotalTime by remember.server(4, 0) { tag -> tag.getIntArray("furnace_progress_data")[1] }
        val cookingTimer by remember.server(5, 0) { tag -> tag.getIntArray("furnace_progress_data")[2] }
        val cookingTotalTime by remember.server(6, 0) { tag -> tag.getIntArray("furnace_progress_data")[3] }
        val progressBar = remember.keep(7, IRenderElement::ProgressData)

        progressBar.current(cookingTimer).max(cookingTotalTime)

        builder.single("working") {
            if (!item0.isEmpty) itemStack(item0)
            if (!item1.isEmpty) itemStack(item1)
            if (litTimeRemaining != 0) {
                energyBar(progressBar)
            }
            if (!item2.isEmpty) itemStack(item2)
        }
        builder.single("fluid") {
            fluid(progressBar, Fluids.WATER.fluidType)
        }
        builder.single("lava") {
            fluid(progressBar, Fluids.LAVA.fluidType)
        }
        builder.single("arrow") {
            workingArrowProgress(progressBar)
        }
        builder.single("cycle") {
            workingCycleProgress(progressBar)
        }
        builder.single("torus") {
            workingTorusProgress(progressBar)
        }
    }

    override fun targetClass(): Class<*> = AbstractFurnaceBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("abstract_furnace_block")

    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
    ): Boolean {
        val furnace = context.getBlockEntity<AbstractFurnaceBlockEntity>() ?: return true
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

        return true
    }
}
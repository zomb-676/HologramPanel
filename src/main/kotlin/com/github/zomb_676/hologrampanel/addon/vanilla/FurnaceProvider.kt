package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.material.Fluids

data object  FurnaceProvider : ServerDataProvider<BlockHologramContext, AbstractFurnaceBlock> {
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
        val progressBar = remember.keep(7, ::ProgressData)

        progressBar.current(cookingTimer).max(cookingTotalTime)

        builder.single("working") {
            if (!item0.isEmpty) itemStack(item0)
            if (!item1.isEmpty) itemStack(item1)
            if (litTimeRemaining != 0) {
                workingArrowProgress(progressBar).setPositionOffset(0, 2)
            }
            if (!item2.isEmpty) itemStack(item2)
        }
    }

    override fun targetClass(): Class<AbstractFurnaceBlock> = AbstractFurnaceBlock::class.java

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

    override fun replaceProvider(target: ResourceLocation): Boolean =
        target == UniversalContainerBlockProvider.location()
}
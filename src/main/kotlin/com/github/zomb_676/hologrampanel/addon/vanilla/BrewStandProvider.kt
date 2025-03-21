package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.BrewingStandBlock
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity

data object  BrewStandProvider : ServerDataProvider<BlockHologramContext, BrewingStandBlock> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val brewStand = context.getBlockEntity<BrewingStandBlockEntity>() ?: return true
        targetData.putInt("brew_time", brewStand.brewTime)
        targetData.putInt("fuel", brewStand.fuel)
        val registryAccess = context.getLevel().registryAccess()
        val items = brewStand.items
        targetData.put("item0", items[0].saveOptional(registryAccess))
        targetData.put("item1", items[1].saveOptional(registryAccess))
        targetData.put("item2", items[2].saveOptional(registryAccess))
        targetData.put("item3", items[3].saveOptional(registryAccess))
        targetData.put("item4", items[4].saveOptional(registryAccess))
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val remember = context.getRememberData()
        val breeTime by remember.server(0, 0) { tag -> tag.getInt("brew_time") }
        val fuel by remember.server(1, -1) { tag -> tag.getInt("fuel") }
        val item0 by remember.serverItemStack(2, "item0")
        val item1 by remember.serverItemStack(3, "item1")
        val item2 by remember.serverItemStack(4, "item2")
        val item3 by remember.serverItemStack(5, "item3")
        val item4 by remember.serverItemStack(6, "item4")

        builder.single("brew_fuel") { text("fuel:$fuel") }
        builder.single("brew_time") { text("breeTime:$breeTime") }
        builder.single("brew_items") {
            if (!item0.isEmpty) itemStack(item0)
            if (!item1.isEmpty) itemStack(item1)
            if (!item2.isEmpty) itemStack(item2)
            if (!item3.isEmpty) itemStack(item3)
            if (!item4.isEmpty) itemStack(item4)
        }
    }

    override fun targetClass(): Class<BrewingStandBlock> = BrewingStandBlock::class.java
    override fun location(): ResourceLocation = HologramPanel.Companion.rl("brewing_stand")

    override fun replaceProvider(target: ResourceLocation): Boolean =
        target == UniversalContainerBlockProvider.location()
}
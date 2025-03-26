package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.saveOptional
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.block.entity.CampfireBlockEntity

data object  CampfireProvider : ServerDataProvider<BlockHologramContext, CampfireBlock> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val campfire = context.getBlockEntity<CampfireBlockEntity>() ?: return true
        targetData.putIntArray("cooking_progress", campfire.cookingProgress)
        targetData.putIntArray("cooking_time", campfire.cookingTime)
        val items = campfire.items
        val registryAccess = context.getLevel().registryAccess()
        targetData.put("item0", items[0].saveOptional(registryAccess))
        targetData.put("item1", items[1].saveOptional(registryAccess))
        targetData.put("item2", items[2].saveOptional(registryAccess))
        targetData.put("item3", items[3].saveOptional(registryAccess))
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val remember = context.getRememberData()
        val cookingProgress by remember.server(
            0,
            intArrayOf(0, 0, 0, 0)
        ) { tag -> tag.getIntArray("cooking_progress").get() }
        val cookingTime by remember.server(
            1,
            intArrayOf(0, 0, 0, 0)
        ) { tag -> tag.getIntArray("cooking_time").get() }
        val item0 by remember.serverItemStack(2, "item0")
        val item1 by remember.serverItemStack(3, "item1")
        val item2 by remember.serverItemStack(4, "item2")
        val item3 by remember.serverItemStack(5, "item3")

        if (!item0.isEmpty) {
            builder.single("cooking0") {
                itemStack(item0)
                text("progress:${cookingProgress[0]}/time:${cookingTime[0]}")
            }
        }
        if (!item1.isEmpty) {
            builder.single("cooking1") {
                itemStack(item1)
                text("progress:${cookingProgress[1]}/time:${cookingTime[1]}")
            }
        }
        if (!item2.isEmpty) {
            builder.single("cooking2") {
                itemStack(item2)
                text("progress:${cookingProgress[2]}/time:${cookingTime[2]}")
            }
        }
        if (!item3.isEmpty) {
            builder.single("cooking3") {
                itemStack(item3)
                text("progress:${cookingProgress[3]}/time:${cookingTime[3]}")
            }
        }
    }

    override fun targetClass(): Class<CampfireBlock> = CampfireBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("camp_fire")

    override fun replaceProvider(target: ResourceLocation): Boolean =
        target == UniversalContainerBlockProvider.location()
}
package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.LecternBlock
import net.minecraft.world.level.block.entity.LecternBlockEntity

data object LecternProvider : ServerDataProvider<BlockHologramContext, LecternBlock> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val lectern = context.getBlockEntity<LecternBlockEntity>() ?: return false
        if (lectern.hasBook()) {
            val item = lectern.book
            targetData.put("book", item.saveOptional(context.getRegistryAccess()))
            return true
        } else {
            targetData.put("book", CompoundTag())
        }
        return false
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val book = builder.context.getRememberData().serverItemStack(0, "book")
        val bookItem = book.get()
        if (!bookItem.isEmpty) {
            builder.single("book") {
                itemStack("lectern_item_logo",bookItem).smallItem()
                component("lectern_item",bookItem.displayName)
            }
        }
    }

    override fun targetClass(): Class<LecternBlock> = LecternBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("lectern_book")

}
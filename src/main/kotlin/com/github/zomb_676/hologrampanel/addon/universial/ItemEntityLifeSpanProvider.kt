package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.item.ItemEntity

data object ItemEntityLifeSpanProvider : ServerDataProvider<EntityHologramContext, ItemEntity> {
    override fun appendComponent(
        builder: HologramWidgetBuilder<EntityHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val remember = context.getRememberData()

        val lifeSpan by remember.server(0, 0) { tag -> tag.getInt("life_span") }
        val age by remember.server(1, -1) { tag -> tag.getInt("age") }

        if (age != -1) {
            builder.single("item_life_span") { text("remain:${lifeSpan - age} Ticks") }
        }
    }

    override fun targetClass(): Class<ItemEntity> = ItemEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("item_entity")

    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: EntityHologramContext
    ): Boolean {
        val itemEntity = context.getEntity<ItemEntity>() ?: return true
        targetData.putInt("life_span", itemEntity.lifespan)
        targetData.putInt("age", itemEntity.age)
        return true
    }

}
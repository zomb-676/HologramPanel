package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag

data object ItemEntityTooltipProvider : ComponentProvider<EntityHologramContext, ItemEntity> {
    override fun appendComponent(
        builder: HologramWidgetBuilder<EntityHologramContext>,
        displayType: DisplayType
    ) {

        val context = builder.context
        val item = context.getEntity<ItemEntity>()?.item ?: return
        val tooltips by context.getRememberData().client(0, listOf()) {
            item.getTooltipLines(context.getPlayer(), TooltipFlag.ADVANCED)
        }

        builder.single("tool") {
            val limit = Config.Client.tooltipLimitHeight.get()
            val tooltip = screenTooltip("item_tooltip", item)
            if (limit > 0) {
                tooltip.setLimitHeight(limit)
            }
        }
    }

    override fun targetClass(): Class<ItemEntity> = ItemEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("item_entity_tooltip")

}
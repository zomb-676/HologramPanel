package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.util.TooltipType
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.client.Minecraft
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
            item.getTooltipLines(Item.TooltipContext.of(context.getLevel()), context.getPlayer(), TooltipFlag.ADVANCED)
        }

        when (Config.Style.itemTooltipType.get()) {
            TooltipType.TEXT -> {
                builder.group("screenTooltip", "screenTooltip") {
                    tooltips.forEachIndexed { index, tooltip ->
                        if (Minecraft.getInstance().font.width(tooltip) > 0) {
                            builder.single("tooltip_$index") { component(tooltip) }
                        }
                    }
                }
            }

            else -> builder.single("tool") {
                screenTooltip(item)
            }
        }
    }

    override fun targetClass(): Class<ItemEntity> = ItemEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("item_entity_tooltip")

}
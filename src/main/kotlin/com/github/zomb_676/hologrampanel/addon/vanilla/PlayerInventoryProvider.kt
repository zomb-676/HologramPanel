package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerEntityProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player

object PlayerInventoryProvider : ServerDataProvider<EntityHologramContext, Player> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: EntityHologramContext
    ): Boolean {
        if (!context.getPlayer().isCreative) return false
        return UniversalContainerEntityProvider.appendServerData(additionData, targetData, context)
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<EntityHologramContext>,
        displayType: DisplayType
    ) {
        return UniversalContainerEntityProvider.appendComponent(builder, displayType)
    }

    override fun targetClass(): Class<Player> = Player::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("player_inventory")

    override fun replaceProvider(target: ResourceLocation): Boolean =
        UniversalContainerEntityProvider.location() == target
}
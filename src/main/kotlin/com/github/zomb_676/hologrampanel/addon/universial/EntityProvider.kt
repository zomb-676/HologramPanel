package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

data object EntityProvider : ServerDataProvider<EntityHologramContext, LivingEntity> {
    override fun appendComponent(
        builder: HologramWidgetBuilder<EntityHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val entity = context.getEntity<LivingEntity>() ?: return
        val remember = context.getRememberData()
        val currentHealth by remember.server(0, -1.0f) { tag -> tag.getFloat("current_health") }
        builder.single("health") {
            heart()
            text("health:${currentHealth}/${entity.maxHealth}")
        }
    }

    override fun targetClass(): Class<LivingEntity> = LivingEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("health_display")

    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: EntityHologramContext
    ): Boolean {
        val entity = context.getEntity<LivingEntity>() ?: return true
        targetData.putFloat("current_health", entity.health)
        return true
    }

}
package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.trans.TransHandle
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
data object UniversalEnergyEntityProvider : ServerDataProvider<EntityHologramContext, Entity> {
    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: EntityHologramContext
    ): Boolean {
        val cap = TransHandle.EntityEnergyTransHandle.getHandle(context.getEntity()) ?: return false
        targetData.putInt("energy_stored", cap.energyStored)
        targetData.putInt("energy_max", cap.maxEnergyStored)
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<EntityHologramContext>, displayType: DisplayType
    ) {
        val remember = builder.context.getRememberData()
        val energyMax by remember.server(0, 0) { tag -> tag.getInt("energy_max") }
        val energyStored by remember.server(1, 0) { tag -> tag.getInt("energy_stored") }
        val progress = remember.keep(2, ::ProgressData)
        if (energyMax > 0) {
            builder.single("energy") {
                progress.current(energyStored).max(energyMax)
                energyBar("entity_energy", progress)
            }
        }
    }

    override fun targetClass(): Class<Entity> = Entity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_energy_entity")

    override fun appliesTo(context: EntityHologramContext, check: Entity) : Boolean =
        TransHandle.EntityEnergyTransHandle.hasHandle(context.getEntity())
}
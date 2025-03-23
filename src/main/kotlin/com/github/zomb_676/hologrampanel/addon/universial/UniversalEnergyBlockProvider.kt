package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.capabilities.Capabilities

data object UniversalEnergyBlockProvider : ServerDataProvider<BlockHologramContext, BlockEntity> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val be = context.getBlockEntity() ?: return false
        val cap = be.level?.getCapability(Capabilities.EnergyStorage.BLOCK, be.blockPos, be.blockState, be, null)
            ?: return false
        targetData.putInt("energy_stored", cap.energyStored)
        targetData.putInt("energy_max", cap.maxEnergyStored)
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val remember = builder.context.getRememberData()
        val energyMax by remember.server(0, 0) { tag -> tag.getInt("energy_max") }
        val energyStored by remember.server(0, 0) { tag -> tag.getInt("energy_stored") }
        val progress = remember.keep(0, ::ProgressData)
        if (energyMax > 0) {
            builder.single("energy") {
                progress.current(energyStored).max(energyMax)
                energyBar(progress)
            }
        }
    }

    override fun targetClass(): Class<BlockEntity> = BlockEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_energy_block")

    override fun appliesTo(
        context: BlockHologramContext,
        check: BlockEntity
    ): Boolean {
        val level = context.getLevel()
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, check.blockPos, check.blockState, check, null) != null
    }
}
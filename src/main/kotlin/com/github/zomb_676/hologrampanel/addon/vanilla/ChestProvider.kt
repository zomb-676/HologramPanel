package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.properties.ChestType

object ChestProvider : ServerDataProvider<BlockHologramContext, ChestBlockEntity> by UniversalContainerBlockProvider {

    override fun targetClass(): Class<ChestBlockEntity> = ChestBlockEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("chest_provider")

    override fun replaceProvider(target: ResourceLocation): Boolean = UniversalContainerBlockProvider.location() == target

    override fun considerShare(): Boolean = true

    override fun exposeSharedTarget(context: BlockHologramContext): Any? {
        context.getBlockEntity<ChestBlockEntity>() ?: return null
        return Object()
    }

    override fun isTargetSame(selfContext: BlockHologramContext, checkContext: BlockHologramContext, checkTarget: Any): Boolean {
        val checkState = checkContext.getBlockState()
        if (checkState.block !is ChestBlock) return false
        if (checkState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return false
        val selfState = selfContext.getBlockState()
        if (selfState.block !is ChestBlock) return false
        if (selfState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return false
        val direction = ChestBlock.getConnectedDirection(checkState)
        return checkContext.pos.relative(direction) == selfContext.pos
    }
}
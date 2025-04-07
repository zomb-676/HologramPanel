package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramWorldContext
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Block

sealed interface DefaultProvider<C : HologramWorldContext, V : Any> : ComponentProvider<C, V> {
    fun getDefaultTarget(context : C) : V

    interface DefaultBlockProvider : DefaultProvider<BlockHologramContext, Block> {
        override fun getDefaultTarget(context: BlockHologramContext): Block = context.getBlockState().block
    }
    interface DefaultEntityProvider : DefaultProvider<EntityHologramContext, Entity> {
        override fun getDefaultTarget(context: EntityHologramContext): Entity = context.getEntity()
    }

}
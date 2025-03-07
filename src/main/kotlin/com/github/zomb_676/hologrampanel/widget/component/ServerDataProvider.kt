package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.AbstractFurnaceBlock

interface ServerDataProvider<T : HologramContext> {
    /**
     * run on logic server
     */
    fun appendServerData(additionData: CompoundTag, targetData: CompoundTag, context: T)

    /**
     * run on logic client, this only create once
     */
    fun additionInformationForServer(additionData: CompoundTag, context : T) {}
}

object FurnaceProvider : ComponentProvider<BlockHologramContext>, ServerDataProvider<BlockHologramContext> {
    override fun targetClass(): Class<*> = AbstractFurnaceBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("abstract_furnace")

    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ) {
        TODO("Not yet implemented")
    }

    override fun appendComponent(builder: HologramWidgetBuilder<BlockHologramContext>) {
        TODO("Not yet implemented")
    }

    override fun additionInformationForServer(additionData: CompoundTag, context: BlockHologramContext) {

    }

}
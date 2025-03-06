package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import net.minecraft.world.level.block.Block

interface ComponentProvider<T : HologramContext> {
    fun appendComponent(builder: HologramWidgetBuilder<T>)

    @EfficientConst
    fun targetClass(): Class<*>
}

object Block : ComponentProvider<BlockHologramContext> {
    override fun appendComponent(builder: HologramWidgetBuilder<BlockHologramContext>) {
        val context = builder.context
        builder.single { this.text { "1" } }
    }

    override fun targetClass(): Class<Block> = Block::class.java
}
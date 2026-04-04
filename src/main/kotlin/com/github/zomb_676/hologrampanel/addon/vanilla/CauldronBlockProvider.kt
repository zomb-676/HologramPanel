package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.AbstractCauldronBlock
import net.neoforged.neoforge.capabilities.Capabilities

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
data object CauldronBlockProvider : ComponentProvider<BlockHologramContext, AbstractCauldronBlock> {
    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val level = context.getLevel()
        val cap = level.getCapability(Capabilities.Fluid.BLOCK, context.pos, null)
        val amount by context.getRememberData().client(0, 0) {
            level.getCapability(Capabilities.Fluid.BLOCK, context.pos, null)?.getAmountAsInt(0) ?: 0
        }
        if (cap != null) {
            val progress = context.getRememberData().keep(1, ::ProgressData)
            val fluidResource = cap.getResource(0)
            if (!fluidResource.isEmpty) {
                progress.current(amount).max(cap.getCapacityAsInt(0, fluidResource))
                builder.single("fluid") {
                    fluid("cauldron_fluid", progress, fluidResource.fluidType)
                }
            }
        }
    }

    override fun targetClass(): Class<AbstractCauldronBlock> = AbstractCauldronBlock::class.java
    override fun location(): Identifier = HologramPanel.rl("cauldron")
}
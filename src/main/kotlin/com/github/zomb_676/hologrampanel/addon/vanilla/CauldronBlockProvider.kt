package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.resources.ResourceLocation
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
        val cap = level.getCapability(Capabilities.FluidHandler.BLOCK, context.pos, null)
        if (cap != null) {
            val progress = context.getRememberData().keep(1, ::ProgressData)
            val fluidStack = cap.getFluidInTank(0)
            if (!fluidStack.isEmpty) {
                progress.current(fluidStack.amount).max(cap.getTankCapacity(0))
                builder.single("fluid") {
                    fluid(progress, fluidStack.fluidType)
                }
            }
        }
    }

    override fun targetClass(): Class<AbstractCauldronBlock> = AbstractCauldronBlock::class.java
    override fun location(): ResourceLocation = HologramPanel.rl("cauldron")
}
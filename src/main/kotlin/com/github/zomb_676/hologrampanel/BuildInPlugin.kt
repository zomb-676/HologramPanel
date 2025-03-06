package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.HologramCommonRegistration
import com.github.zomb_676.hologrampanel.api.HologramPlugin
import com.github.zomb_676.hologrampanel.api.IHologramPlugin
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block

@HologramPlugin
class BuildInPlugin : IHologramPlugin {
    override fun location(): ResourceLocation = HologramPanel.rl("build_in")

    override fun registerCommon(register: HologramCommonRegistration) {
        register.registerBlockComponent<Block>("block") { builder ->
            builder.single { component { getBlockState().block.name } }
        }
    }
}
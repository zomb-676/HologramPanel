package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.FluidDataSyncEntry
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.capabilities.Capabilities

data object  UniversalFluidBlockProvider : ServerDataProvider<BlockHologramContext, BlockEntity> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val be = context.getBlockEntity() ?: return false
        val cap = be.level?.getCapability(Capabilities.FluidHandler.BLOCK, be.blockPos, be.blockState, be, null)
            ?: return false
        val buffer = context.createRegistryFriendlyByteBuf()
        var fluidCount = 0
        repeat(cap.tanks) { index ->
            val fluidStack = cap.getFluidInTank(index)
            if (!fluidStack.isEmpty) {
                val entry = FluidDataSyncEntry(fluidStack.fluidType, fluidStack.amount, cap.getTankCapacity(index))
                FluidDataSyncEntry.STREAM_CODEC.encode(buffer, entry)
                fluidCount++
            }
        }
        targetData.putByteArray("fluid_data", buffer.array())
        targetData.putInt("fluid_count", fluidCount)
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val remember = builder.context.getRememberData()
        val fluids by remember.server(0, listOf()) { tag ->
            val count = tag.getInt("fluid_count")
            val buffer = context.warpRegistryFriendlyByteBuf(tag.getByteArray("fluid_data"))
            List(count) {
                FluidDataSyncEntry.STREAM_CODEC.decode(buffer)
            }
        }
        val progresses = remember.keep(0) { mutableListOf<ProgressData>() }
        while (progresses.size < fluids.size) {
            progresses.add(ProgressData())
        }
        if (fluids.isNotEmpty()) {
            builder.group("fluids", "fluids") {
                fluids.forEachIndexed { index, fluid ->
                    builder.single("fluid_$index") {
                        val progress = progresses[index]
                        progress.current(fluid.current).max(fluid.max)
                        fluid(progress, fluid.type)
                    }
                }
            }
        }
    }

    override fun targetClass(): Class<BlockEntity> = BlockEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_fluid_block")
}
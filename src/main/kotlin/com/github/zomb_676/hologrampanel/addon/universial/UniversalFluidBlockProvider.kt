package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.FluidDataSyncEntry
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.ForgeCapabilities

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
data object UniversalFluidBlockProvider : ServerDataProvider<BlockHologramContext, BlockEntity> {
    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
    ): Boolean {
        val cap = context.getBlockEntity()?.getCapability(ForgeCapabilities.FLUID_HANDLER)?.orElse(null) ?: return false
        val buffer = context.createFriendlyByteBuf()
        var fluidCount = 0
        repeat(cap.tanks) { index ->
            val fluidStack = cap.getFluidInTank(index)
            if (!fluidStack.isEmpty) {
                val entry = FluidDataSyncEntry(fluidStack, cap.getTankCapacity(index))
                FluidDataSyncEntry.STREAM_CODEC.encode(buffer, entry)
                fluidCount++
            }
        }
        targetData.putByteArray("fluid_data", buffer.extractArray())
        targetData.putInt("fluid_count", fluidCount)
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>, displayType: DisplayType
    ) {
        val context = builder.context
        val remember = builder.context.getRememberData()
        val fluids by remember.server(0, listOf()) { tag ->
            val count = tag.getInt("fluid_count")
            val buffer = context.warpFriendlyByteBuf(tag.getByteArray("fluid_data"))
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
                        fluid("block_fluid", progress, fluid.fluidStack.fluid.fluidType)
                    }
                }
            }
        }
    }

    override fun targetClass(): Class<BlockEntity> = BlockEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_fluid_block")

    override fun appliesTo(
        context: BlockHologramContext, check: BlockEntity
    ): Boolean {
        return check.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent
    }
}
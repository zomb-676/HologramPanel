package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.util.FluidDataSyncEntry
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.neoforge.capabilities.Capabilities

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
data object UniversalFluidEntityProvider : ServerDataProvider<EntityHologramContext, Entity> {
    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: EntityHologramContext
    ): Boolean {
        val entity = context.getEntity()
        val cap = entity.getCapability(Capabilities.FluidHandler.ENTITY, null) ?: return false
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
        targetData.putByteArray("fluid_data", buffer.extractArray())
        targetData.putInt("fluid_count", fluidCount)
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<EntityHologramContext>, displayType: DisplayType
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
                        fluid("entity_fluid", progress, fluid.type)
                    }
                }
            }
        }
    }

    override fun targetClass(): Class<Entity> = Entity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_fluid_entity")

    override fun appliesTo(
        context: EntityHologramContext, check: Entity
    ): Boolean {
        return check.getCapability(Capabilities.FluidHandler.ENTITY, null) != null
    }
}
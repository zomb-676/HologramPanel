package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.BrewingStandBlock
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import java.util.Arrays

data object BrewStandProvider : ServerDataProvider<BlockHologramContext, BrewingStandBlock> {

    const val BREW_TOTAL_TIME = 400

    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
    ): Boolean {
        val brewStand = context.getBlockEntity<BrewingStandBlockEntity>() ?: return true
        val buffer = context.createRegistryFriendlyByteBuf()
        buffer.writeVarInt(brewStand.brewTime)
        buffer.writeVarInt(brewStand.fuel)
        brewStand.items.forEach {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, it)
        }
        targetData.putByteArray("brew", buffer.extractArray())
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>, displayType: DisplayType
    ) {
        val context = builder.context
        val remember = context.getRememberData()
        val data by remember.server(0, ByteArray(0), Arrays::equals) { tag -> tag.getByteArray("brew") }
        val progress = remember.keep(2) { ProgressData().max(BREW_TOTAL_TIME) }
        if (data.isEmpty()) return
        val buffer = context.warpRegistryFriendlyByteBuf(data)
        val brewTime = buffer.readVarInt()
        val fuel = buffer.readVarInt()
        val items = List(5) {
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)
        }

        builder.single("brew_state") {
            itemInteractive(items[4], 4)
            text("fuel:$fuel").setPositionOffset(0, 4)
        }
        builder.single("brew_items") {
            if (brewTime > 0) {
                progress.current(BREW_TOTAL_TIME - brewTime)
                workingTorusProgress(progress).noCalculateSize().setScale(0.8)
            }
            itemInteractive(items[3], 3)
            itemInteractive(items[0], 0)
            itemInteractive(items[1], 1)
            itemInteractive(items[2], 2)
        }
    }

    override fun targetClass(): Class<BrewingStandBlock> = BrewingStandBlock::class.java
    override fun location(): ResourceLocation = HologramPanel.Companion.rl("brewing_stand")

    override fun replaceProvider(target: ResourceLocation): Boolean = target == UniversalContainerBlockProvider.location()
}
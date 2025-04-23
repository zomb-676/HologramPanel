package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.trans.TransHandle
import com.github.zomb_676.hologrampanel.trans.TransSource
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.BrewingStandBlock
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import java.util.*

data object BrewStandProvider : ServerDataProvider<BlockHologramContext, BrewingStandBlock> {

    const val BREW_TOTAL_TIME = 400

    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
    ): Boolean {
        val brewStand = context.getBlockEntity<BrewingStandBlockEntity>() ?: return true
        val buffer = context.createFriendlyByteBuf()
        buffer.writeVarInt(brewStand.brewTime)
        buffer.writeVarInt(brewStand.fuel)
        brewStand.items.forEach {
            ByteBufCodecs.ITEM_STACK.encode(buffer, it)
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
        val source = remember.keep(3) {
            TransSource.create(context.getBlockEntity()!!)
        }
        if (data.isEmpty()) return
        val buffer = context.warpFriendlyByteBuf(data)
        val brewTime = buffer.readVarInt()
        val fuel = buffer.readVarInt()
        val items = List(5) {
            ByteBufCodecs.ITEM_STACK.decode(buffer)
        }

        builder.single("brew_state") {
            itemInteractive("brew_items", items[4], 4, source, TransHandle.BlockItemTransHandle)
            text("brew_fuel", "fuel:$fuel").setPositionOffset(0, 4)
        }
        builder.single("brew_items") {
            if (brewTime > 0) {
                progress.current(BREW_TOTAL_TIME - brewTime)
                workingTorusProgress("brew_progress", progress).noCalculateSize().setScale(0.8)
            }
            itemInteractive("item3", items[3], 3, source, TransHandle.BlockItemTransHandle)
            itemInteractive("item0", items[0], 0, source, TransHandle.BlockItemTransHandle)
            itemInteractive("item1", items[1], 1, source, TransHandle.BlockItemTransHandle)
            itemInteractive("item2", items[2], 2, source, TransHandle.BlockItemTransHandle)
        }
    }

    override fun targetClass(): Class<BrewingStandBlock> = BrewingStandBlock::class.java
    override fun location(): ResourceLocation = HologramPanel.Companion.rl("brewing_stand")

    override fun replaceProvider(target: ResourceLocation): Boolean = target == UniversalContainerBlockProvider.location()
}
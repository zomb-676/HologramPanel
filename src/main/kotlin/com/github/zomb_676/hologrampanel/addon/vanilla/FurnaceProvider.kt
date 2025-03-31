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
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity

data object FurnaceProvider : ServerDataProvider<BlockHologramContext, AbstractFurnaceBlock> {

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val remember = builder.context.getRememberData()
        val data by remember.server(0, ByteArray(0), ByteArray::equals) { tag -> tag.getByteArray("f") }
        val progressBar = remember.keep(1, ::ProgressData)

        if (data.isEmpty()) return
        val buffer = builder.context.warpRegistryFriendlyByteBuf(data)
        val item0 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)
        val item1 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)
        val item2 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)
        val litTimeRemaining = buffer.readVarInt()
        val litTotalTime = buffer.readVarInt()
        val cookingTimer = buffer.readVarInt()
        val cookingTotalTime = buffer.readVarInt()

        progressBar.current(cookingTimer).max(cookingTotalTime)

        builder.single("working") {
            itemInteractive(item0, 0)
            itemInteractive(item1, 1)
            if (litTimeRemaining != 0) {
                workingArrowProgress(progressBar).setPositionOffset(0, 2)
            }
            if (!item2.isEmpty) itemInteractive(item2, 2)
        }
    }

    override fun targetClass(): Class<AbstractFurnaceBlock> = AbstractFurnaceBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("abstract_furnace_block")

    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
    ): Boolean {
        val furnace = context.getBlockEntity<AbstractFurnaceBlockEntity>() ?: return true
        val buffer = context.createRegistryFriendlyByteBuf()
        furnace.items.forEach {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, it)
        }
        buffer.writeVarInt(furnace.litTimeRemaining)
        buffer.writeVarInt(furnace.litTotalTime)
        buffer.writeVarInt(furnace.cookingTimer)
        buffer.writeVarInt(furnace.cookingTotalTime)
        targetData.putByteArray("f", buffer.extractArray())
        return true
    }

    override fun replaceProvider(target: ResourceLocation): Boolean =
        target == UniversalContainerBlockProvider.location()
}
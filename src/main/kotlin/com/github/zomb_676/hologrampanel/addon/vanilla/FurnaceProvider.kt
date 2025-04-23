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
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import java.util.*

data object FurnaceProvider : ServerDataProvider<BlockHologramContext, AbstractFurnaceBlock> {

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val remember = builder.context.getRememberData()
        val data by remember.server(0, ByteArray(0), Arrays::equals) { tag -> tag.getByteArray("f") }
        val progressBar = remember.keep(1, ::ProgressData)
        val source = remember.keep(2) {
            TransSource.create(builder.context.getBlockEntity()!!)
        }

        if (data.isEmpty()) return
        val buffer = builder.context.warpFriendlyByteBuf(data)
        val item0 = ByteBufCodecs.ITEM_STACK.decode(buffer)
        val item1 = ByteBufCodecs.ITEM_STACK.decode(buffer)
        val item2 = ByteBufCodecs.ITEM_STACK.decode(buffer)
        val litTimeRemaining = buffer.readVarInt()
        buffer.readVarInt()
        val cookingTimer = buffer.readVarInt()
        val cookingTotalTime = buffer.readVarInt()

        progressBar.current(cookingTimer).max(cookingTotalTime)

        builder.single("working") {
            itemInteractive("furnace_input_0", item0, 0, source, TransHandle.BlockItemTransHandle)
            itemInteractive("furnace_input_1", item1, 1, source, TransHandle.BlockItemTransHandle)
            if (litTimeRemaining != 0) {
                workingArrowProgress("furnace_progress", progressBar).setPositionOffset(1, 2)
            }
            if (!item2.isEmpty) itemInteractive("furnace_output", item2, 2, source, TransHandle.BlockItemTransHandle)
        }
    }

    override fun targetClass(): Class<AbstractFurnaceBlock> = AbstractFurnaceBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("abstract_furnace_block")

    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
    ): Boolean {
        val furnace = context.getBlockEntity<AbstractFurnaceBlockEntity>() ?: return true
        val buffer = context.createFriendlyByteBuf()
        furnace.items.forEach {
            ByteBufCodecs.ITEM_STACK.encode(buffer, it)
        }
        buffer.writeVarInt(furnace.litTime)
        buffer.writeVarInt(furnace.litDuration)
        buffer.writeVarInt(furnace.cookingProgress)
        buffer.writeVarInt(furnace.cookingTotalTime)
        targetData.putByteArray("f", buffer.extractArray())
        return true
    }

    override fun replaceProvider(target: ResourceLocation): Boolean =
        target == UniversalContainerBlockProvider.location()
}
package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.capabilities.Capabilities

data object  UniversalContainerBlockProvider : ServerDataProvider<BlockHologramContext, BlockEntity> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val be = context.getBlockEntity() ?: return false
        val cap = be.level?.getCapability(Capabilities.ItemHandler.BLOCK, be.blockPos, be.blockState, be, null)
            ?: return false
        val buffer = context.createRegistryFriendlyByteBuf()
        var writeItemCount = 0
        repeat(cap.slots) { index ->
            val item = cap.getStackInSlot(index)
            if (!item.isEmpty) {
                ItemStack.STREAM_CODEC.encode(buffer, item)
                writeItemCount++
            }
        }
        targetData.putInt("item_count", writeItemCount)
        targetData.putByteArray("item_data", buffer.array())
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val remember = builder.context.getRememberData()
        val items by remember.server(0, listOf()) { tag ->
            val count = tag.getInt("item_count")
            val buffer = context.warpRegistryFriendlyByteBuf(tag.getByteArray("item_data"))
            List(count) {
                ItemStack.STREAM_CODEC.decode(buffer)
            }
        }
        if (items.isNotEmpty()) {
            builder.single("items") {
                items(items)
            }
        }
    }

    override fun targetClass(): Class<BlockEntity> = BlockEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_container_block")
}
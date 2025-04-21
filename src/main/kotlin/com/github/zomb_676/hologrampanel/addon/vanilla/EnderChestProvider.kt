package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.EnderChestBlock
import net.neoforged.neoforge.items.wrapper.InvWrapper

data object EnderChestProvider : ServerDataProvider<BlockHologramContext, EnderChestBlock> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val player = context.getPlayer()
        val inventory = player.enderChestInventory ?: return false
        val wrapper = InvWrapper(inventory)
        val buffer = context.createRegistryFriendlyByteBuf()
        var writeItemCount = 0
        repeat(wrapper.slots) { index ->
            val item = wrapper.getStackInSlot(index)
            if (!item.isEmpty) {
                ItemStack.STREAM_CODEC.encode(buffer, item)
                writeItemCount++
            }
        }
        targetData.putInt("item_count", writeItemCount)
        targetData.putByteArray("item_data", buffer.extractArray())
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
                items("ender_chest_items", items)
            }
        }
    }

    override fun targetClass(): Class<EnderChestBlock> = EnderChestBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("ender_chest")

    override fun replaceProvider(target: ResourceLocation): Boolean =
        target == UniversalContainerBlockProvider.location()
}
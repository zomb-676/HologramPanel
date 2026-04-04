package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.EnderChestBlock
import net.neoforged.neoforge.transfer.item.ItemUtil
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper

data object EnderChestProvider : ServerDataProvider<BlockHologramContext, EnderChestBlock> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val player = context.getPlayer()
        val inventory = player.enderChestInventory ?: return false
        val wrapper = VanillaContainerWrapper.of(inventory)
        val buffer = context.createRegistryFriendlyByteBuf()
        var writeItemCount = 0
        repeat(wrapper.size()) { index ->
            val item = ItemUtil.getStack(wrapper, index)
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
            tag.getInt("item_count").flatMap { count ->
                tag.getByteArray("item_data").map { itemData ->
                    val buffer = context.warpRegistryFriendlyByteBuf(itemData)
                    List(count) {
                        ItemStack.STREAM_CODEC.decode(buffer)
                    }
                }
            }
        }
        if (items.isNotEmpty()) {
            builder.single("items") {
                items("ender_chest_items", items)
            }
        }
    }

    override fun targetClass(): Class<EnderChestBlock> = EnderChestBlock::class.java

    override fun location(): Identifier = HologramPanel.rl("ender_chest")

    override fun replaceProvider(target: Identifier): Boolean =
        target == UniversalContainerBlockProvider.location()
}
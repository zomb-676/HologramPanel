package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.npc.InventoryCarrier
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.transfer.item.ItemUtil
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper

data object InventoryCarrierProvider : ServerDataProvider<EntityHologramContext, InventoryCarrier> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: EntityHologramContext
    ): Boolean {
        val carrier = context.getEntity() as? InventoryCarrier? ?: return false
        val inv = VanillaContainerWrapper.of(carrier.inventory)
        val buffer = context.createRegistryFriendlyByteBuf()
        var writeItemCount = 0
        repeat(inv.size()) { index ->
            val item = ItemUtil.getStack(inv, index)
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
        builder: HologramWidgetBuilder<EntityHologramContext>,
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
                items("inv_carrier_items",items)
            }
        }
    }

    override fun targetClass(): Class<InventoryCarrier> = InventoryCarrier::class.java

    override fun location(): Identifier = HologramPanel.rl("inventory_carrier")
}
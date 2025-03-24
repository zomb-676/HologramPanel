package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.npc.InventoryCarrier
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.wrapper.InvWrapper

data object InventoryCarrierProvider : ServerDataProvider<EntityHologramContext, InventoryCarrier> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: EntityHologramContext
    ): Boolean {
        val carrier = context.getEntity() as? InventoryCarrier? ?: return false
        val inv = InvWrapper(carrier.inventory)
        val buffer = context.createRegistryFriendlyByteBuf()
        var writeItemCount = 0
        repeat(inv.slots) { index ->
            val item = inv.getStackInSlot(index)
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
        builder: HologramWidgetBuilder<EntityHologramContext>,
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

    override fun targetClass(): Class<InventoryCarrier> = InventoryCarrier::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("inventory_carrier")
}
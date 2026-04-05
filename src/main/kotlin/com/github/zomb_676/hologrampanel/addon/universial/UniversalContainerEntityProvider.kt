package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.trans.TransHandle
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.transfer.item.ItemUtil

data object UniversalContainerEntityProvider : ServerDataProvider<EntityHologramContext, Entity> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: EntityHologramContext
    ): Boolean {
        val cap = TransHandle.EntityItemTransHandle.getHandleNullable(context.getEntity()) ?: return false
        val buffer = context.createRegistryFriendlyByteBuf()
        var writeItemCount = 0
        repeat(cap.size()) { index ->
            val item = ItemUtil.getStack(cap, index)
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
                tag.getByteArray("item_data").map {
                    val buffer = context.warpRegistryFriendlyByteBuf(it)
                    List(count) {
                        ItemStack.STREAM_CODEC.decode(buffer)
                    }
                }
            }
        }
        if (items.isNotEmpty()) {
            builder.single("items") {
                items("entity_items", items)
            }
        }
    }

    override fun targetClass(): Class<Entity> = Entity::class.java

    override fun location(): Identifier = HologramPanel.rl("universal_container_entity")

    override fun appliesTo(context: EntityHologramContext, check: Entity): Boolean =
        TransHandle.EntityItemTransHandle.hasHandle(check)
}
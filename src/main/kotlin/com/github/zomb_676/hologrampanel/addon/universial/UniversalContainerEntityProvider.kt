package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.trans.TransHandle
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

data object UniversalContainerEntityProvider : ServerDataProvider<EntityHologramContext, Entity> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: EntityHologramContext
    ): Boolean {
        val cap = TransHandle.EntityItemTransHandle.getHandleNullable(context.getEntity()) ?: return false
        val buffer = context.createFriendlyByteBuf()
        var writeItemCount = 0
        repeat(cap.slots) { index ->
            val item = cap.getStackInSlot(index)
            if (!item.isEmpty) {
                ByteBufCodecs.ITEM_STACK.encode(buffer, item)
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
            val count = tag.getInt("item_count")
            val buffer = context.warpFriendlyByteBuf(tag.getByteArray("item_data"))
            List(count) {
                ByteBufCodecs.ITEM_STACK.decode(buffer)
            }
        }
        if (items.isNotEmpty()) {
            builder.single("items") {
                items("entity_items", items)
            }
        }
    }

    override fun targetClass(): Class<Entity> = Entity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_container_entity")

    override fun appliesTo(context: EntityHologramContext, check: Entity): Boolean =
        TransHandle.EntityItemTransHandle.hasHandle(check)
}
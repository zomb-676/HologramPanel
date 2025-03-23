package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities

data object  UniversalContainerEntityProvider : ServerDataProvider<EntityHologramContext, Entity> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: EntityHologramContext
    ): Boolean {
        val entity = context.getEntity()
        val cap = entity.getCapability(Capabilities.ItemHandler.ENTITY) ?: return false
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

    override fun targetClass(): Class<Entity> = Entity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_container_entity")

    override fun appliesTo(
        context: EntityHologramContext,
        check: Entity
    ): Boolean {
        return check.getCapability(Capabilities.ItemHandler.ENTITY, null) != null
    }
}
package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.trans.TransHandle
import com.github.zomb_676.hologrampanel.trans.TransSource
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
data object UniversalContainerBlockProvider : ServerDataProvider<BlockHologramContext, BlockEntity> {
    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
    ): Boolean {
        val cap = TransHandle.BlockItemTransHandle.getHandleNullable(context.getBlockEntity()) ?: return false

        val container: Object2IntOpenCustomHashMap<ItemStack> = Object2IntOpenCustomHashMap(ItemStackWithComponentStrategy)
        repeat(cap.slots) { index ->
            val item = cap.getStackInSlot(index)
            if (item.isEmpty) return@repeat
            container.addTo(item, item.count)
        }

        val buffer = context.createFriendlyByteBuf()
        buffer.writeVarInt(container.size)
        val iterator = container.object2IntEntrySet().fastIterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            encodeItem(buffer, entry.key, entry.intValue)
        }
        targetData.putByteArray("items", buffer.extractArray())
        return true
    }

    /**
     * custom encode function to avoid item instance creation
     *
     * @param itemStack assume not [ItemStack.isEmpty], not use its count
     * @param count use this count
     */
    private fun encodeItem(buffer: RegistryFriendlyByteBuf, itemStack: ItemStack, count: Int) {
        if (itemStack.isEmpty) {
            buffer.writeBoolean(false)
            return
        }
        buffer.writeBoolean(true)
        val item = itemStack.item
        @Suppress("DEPRECATION")
        buffer.writeId(BuiltInRegistries.ITEM, item)
        buffer.writeVarInt(count)
        val tag = if (item.isDamageable(itemStack) || item.shouldOverrideMultiplayerNbt()) {
            itemStack.tag
        } else null
        buffer.writeNbt(tag)
    }

    object ItemStackWithComponentStrategy : Hash.Strategy<ItemStack?> {
        override fun hashCode(itemStack: ItemStack?): Int {
            if (itemStack == null) return 0
            return HashCodeBuilder().append(itemStack.item).append(itemStack.tag?.hashCode()).toHashCode()
        }

        override fun equals(a: ItemStack?, b: ItemStack?): Boolean = if (b == null) a == null else ItemStack.isSameItemSameTags(a, b)
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>, displayType: DisplayType
    ) {
        val context = builder.context
        val remember = builder.context.getRememberData()
        val data by remember.server(0, ByteArray(0), Arrays::equals) { tag ->
            tag.getByteArray("items")
        }
        if (data.isEmpty()) return
        val buffer = context.warpFriendlyByteBuf(data)
        val count = buffer.readVarInt()
        val items = MutableList(count) {
            ByteBufCodecs.ITEM_STACK.decode(buffer)
        }
        if (items.isNotEmpty() || builder.onForceDisplay) {
            items.sortWith(Comparator.comparingInt { BuiltInRegistries.ITEM.getId(it.item) })
            builder.single("items") {
                itemsInteractive("container_items", items, TransSource.create(context.getBlockEntity()!!), TransHandle.BlockItemTransHandle)
            }
        }
    }

    override fun targetClass(): Class<BlockEntity> = BlockEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_container_block")

    override fun appliesTo(context: BlockHologramContext, check: BlockEntity): Boolean {
        val cap = TransHandle.BlockItemTransHandle.getHandle(check)
        return cap != null && cap.slots < 128
    }

    override fun requireRebuildOnForceDisplay(context: BlockHologramContext): Boolean = true
}
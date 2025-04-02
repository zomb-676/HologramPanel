package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.capabilities.Capabilities
import java.util.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
data object UniversalContainerBlockProvider : ServerDataProvider<BlockHologramContext, BlockEntity> {
    override fun appendServerData(
        additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
    ): Boolean {
        val be = context.getBlockEntity() ?: return false
        val cap = be.level?.getCapability(Capabilities.ItemHandler.BLOCK, be.blockPos, be.blockState, be, null)
            ?: return false

        val container: Object2IntOpenCustomHashMap<ItemStack> = Object2IntOpenCustomHashMap(ItemStackWithComponentStrategy)
        repeat(cap.slots) { index ->
            val item = cap.getStackInSlot(index)
            if (item.isEmpty) return@repeat
            container.addTo(item, item.count)
        }

        val buffer = context.createRegistryFriendlyByteBuf()
        buffer.writeVarInt(container.size)
        val iterator = container.object2IntEntrySet().fastIterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            encodeItem(buffer, entry.key, entry.intValue)
        }
        targetData.putByteArray("items", buffer.extractArray())
        return true
    }

    private val ITEM_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> = ByteBufCodecs.holderRegistry<Item>(Registries.ITEM)

    /**
     * custom encode function to avoid item instance creation
     *
     * @param itemStack assume not [ItemStack.isEmpty], not use its count
     * @param count use this count
     */
    private fun encodeItem(buffer: RegistryFriendlyByteBuf, itemStack: ItemStack, count: Int) {
        require(!itemStack.isEmpty)
        buffer.writeVarInt(count)
        ITEM_STREAM_CODEC.encode(buffer, itemStack.itemHolder)
        DataComponentPatch.STREAM_CODEC.encode(buffer, itemStack.components.asPatch())
        ItemStack.OPTIONAL_LIST_STREAM_CODEC
    }

    object ItemStackWithComponentStrategy : Hash.Strategy<ItemStack?> {
        override fun hashCode(itemStack: ItemStack?): Int = ItemStack.hashItemAndComponents(itemStack)
        override fun equals(a: ItemStack?, b: ItemStack?): Boolean = if (b == null) a == null else ItemStack.isSameItemSameComponents(a, b)
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
        val buffer = context.warpRegistryFriendlyByteBuf(data)
        val count = buffer.readVarInt()
        val items = List(count) {
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)
        }

        if (items.isNotEmpty()) {
            builder.single("items") {
                itemsInteractive(items, true)
            }
        }
    }

    override fun targetClass(): Class<BlockEntity> = BlockEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("universal_container_block")

    override fun appliesTo(
        context: BlockHologramContext, check: BlockEntity
    ): Boolean {
        val level = context.getLevel()
        val cap = level.getCapability(
            Capabilities.ItemHandler.BLOCK, check.blockPos, check.blockState, check, null
        )
        return cap != null && cap.slots < 128
    }
}
package com.github.zomb_676.hologrampanel.trans

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler

/**
 * query/store actual/test object in handles
 */
sealed interface TransPath<in H : Any, R : Any> {
    fun extractActual(handle: H): R?
    fun extractTest(handle: H): R?

    fun storeActual(handle: H, store: R): R?
    fun storeTest(handle: H, store: R): R?

    fun setCount(count: Int, obj: R)
    fun getCount(obj: R): Int

    var count: Int

    sealed interface Item : TransPath<IItemHandler, ItemStack> {
        override fun setCount(count: Int, obj: ItemStack) {
            obj.count = count
        }

        override fun getCount(obj: ItemStack): Int = obj.count

        data class ByIndex(val index: Int, override var count: Int, val target: ItemStack = ItemStack.EMPTY) : Item {
            override fun extractActual(handle: IItemHandler): ItemStack? {
                if (index >= 0 && index < handle.slots) {
                    if (!target.isEmpty && !ItemStack.isSameItemSameComponents(target, handle.getStackInSlot(index))) return null
                    return handle.extractItem(index, count, false)
                }
                return null
            }

            override fun extractTest(handle: IItemHandler): ItemStack? {
                if (index >= 0 && index < handle.slots) {
                    if (!target.isEmpty && !ItemStack.isSameItemSameComponents(target, handle.getStackInSlot(index))) return null
                    return handle.extractItem(index, count, true)
                }
                return null
            }

            override fun storeActual(
                handle: IItemHandler,
                store: ItemStack
            ): ItemStack? {
                if (index >= 0 && index < handle.slots) {
                    return handle.insertItem(index, store, false)
                }
                return store
            }

            override fun storeTest(
                handle: IItemHandler,
                store: ItemStack
            ): ItemStack? {
                if (index >= 0 && index < handle.slots) {
                    return handle.insertItem(index, store, true)
                }
                return store
            }

            companion object {
                val STREAM_CODE: StreamCodec<RegistryFriendlyByteBuf, ByIndex> = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ByIndex::index,
                    ByteBufCodecs.VAR_INT, ByIndex::count,
                    ItemStack.OPTIONAL_STREAM_CODEC, ByIndex::target,
                    ::ByIndex
                )
            }
        }

        data class ByItem(val itemStack: ItemStack) : Item {
            override var count: Int = itemStack.count

            override fun extractActual(handle: IItemHandler): ItemStack? {
                var count = 0
                val itemReturn = itemStack.copyWithCount(0)
                for (index in 0..<handle.slots) {
                    if (count < itemStack.count) {
                        if (ItemStack.isSameItemSameComponents(itemStack, handle.getStackInSlot(index))) {
                            count += handle.extractItem(index, itemStack.count - count, true).count
                        }
                    } else break
                }
                return itemReturn
            }

            override fun extractTest(handle: IItemHandler): ItemStack? {
                var count = 0
                val itemReturn = itemStack.copyWithCount(0)
                for (index in 0..<handle.slots) {
                    if (count < itemStack.count) {
                        if (ItemStack.isSameItemSameComponents(itemStack, handle.getStackInSlot(index))) {
                            count += handle.extractItem(index, itemStack.count - count, false).count
                        }
                    } else break
                }
                return itemReturn
            }

            override fun storeActual(
                handle: IItemHandler,
                store: ItemStack
            ): ItemStack {
                var remain = store
                for (index in 0..<handle.slots) {
                    if (remain.count > 0) {
                        remain = handle.insertItem(index, remain, false)
                    } else break
                }
                return remain
            }

            override fun storeTest(
                handle: IItemHandler,
                store: ItemStack
            ): ItemStack? {
                var remain = store
                for (index in 0..<handle.slots) {
                    if (remain.count > 0) {
                        remain = handle.insertItem(index, remain, true)
                    } else break
                }
                return remain
            }

            companion object {
                val STREAM_CODE: StreamCodec<RegistryFriendlyByteBuf, ByItem> = StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC, ByItem::itemStack,
                    ::ByItem
                )
            }
        }

        companion object {
            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Item> = object : StreamCodec<RegistryFriendlyByteBuf, Item> {
                override fun decode(buffer: RegistryFriendlyByteBuf): Item {
                    return when (buffer.readShort().toInt()) {
                        0 -> ByIndex.STREAM_CODE.decode(buffer)
                        1 -> ByItem.STREAM_CODE.decode(buffer)
                        else -> throw RuntimeException()
                    }
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf,
                    value: Item
                ) {
                    when (value) {
                        is ByIndex -> {
                            buffer.writeShort(0)
                            ByIndex.STREAM_CODE.encode(buffer, value)
                        }

                        is ByItem -> {
                            buffer.writeShort(1)
                            ByItem.STREAM_CODE.encode(buffer, value)
                        }
                    }
                }
            }
        }
    }

    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TransPath<*, *>> = object : StreamCodec<RegistryFriendlyByteBuf, TransPath<*, *>> {
            override fun decode(buffer: RegistryFriendlyByteBuf): TransPath<*, *> {
                return when (buffer.readShort().toInt()) {
                    0 -> Item.STREAM_CODEC.decode(buffer)
                    else -> throw RuntimeException()
                }
            }

            override fun encode(
                buffer: RegistryFriendlyByteBuf,
                value: TransPath<*, *>
            ) {
                when (value) {
                    is Item -> {
                        buffer.writeShort(0)
                        Item.STREAM_CODEC.encode(buffer, value)
                    }
                }
            }
        }
    }
}
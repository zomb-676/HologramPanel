package com.github.zomb_676.hologrampanel.trans

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.energy.IEnergyStorage
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.energy.EnergyHandler
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.fluid.FluidUtil
import net.neoforged.neoforge.transfer.item.ItemResource
import net.neoforged.neoforge.transfer.item.ItemUtil
import net.neoforged.neoforge.transfer.transaction.Transaction

/**
 * query/store actual/test object in handles
 *
 * @param H handle type, same in [TransHandle]
 * @param R result type, represent by the data hold by [H]
 */
sealed interface TransPath<in H : Any, R : Any> {

    /**
     * query [R] form [handle] by simulate, pure function
     *
     * @return the extracted
     */
    fun extractActual(handle: H): R

    /**
     * query [R] from [handle] actually, pure function
     *
     * @return the extracted
     */
    fun extractTest(handle: H): R

    /**
     * store [R] form [handle] by simulate, pure function
     *
     * @return the remain part which is not stored
     */
    fun storeActual(handle: H, store: R): R

    /**
     * store [R] from [handle] actually, pure function
     *
     * @return the remain part which is not stored
     */
    fun storeTest(handle: H, store: R): R

    /**
     * helper function for [R]
     *
     * get the count value represented by the [R], pure function
     */
    fun getCount(obj: R): Int

    /**
     * helper function for [R]
     *
     * check if type [R] indicates empty, pure function
     */
    fun isEmpty(obj: R): Boolean

    /**
     * the value that indicates how many which be queried/stored
     */
    var count: Int

    sealed interface Item : TransPath<ResourceHandler<ItemResource>, ItemStack> {

        override fun getCount(obj: ItemStack): Int = obj.count

        override fun isEmpty(obj: ItemStack): Boolean = obj.isEmpty

        fun ResourceHandler<ItemResource>.isInRange(index: Int) = index >= 0 && index < this.size()

        /**
         * operate items at specific slot and
         *
         * @property index slot index
         * @property count operate count
         * @property target work as a filter, use [ItemStack.isSameItemSameComponents]
         */
        data class ByIndex(val index: Int, override var count: Int, val target: ItemStack = ItemStack.EMPTY) : Item {
            override fun extractActual(handle: ResourceHandler<ItemResource>): ItemStack {
                if (handle.isInRange(index)) {
                    if (!target.isEmpty && !ItemStack.isSameItemSameComponents(target, ItemUtil.getStack(handle, index))) return ItemStack.EMPTY
                    return Transaction.openRoot().use {
                        val result = handle.extract(index, ItemResource.of(target.item), count, it)
                        it.commit()
                        ItemStack(target.item, result)
                    }
                }
                return ItemStack.EMPTY
            }

            override fun extractTest(handle: ResourceHandler<ItemResource>): ItemStack {
                if (handle.isInRange(index)) {
                    if (!target.isEmpty && !ItemStack.isSameItemSameComponents(target, ItemUtil.getStack(handle, index))) return ItemStack.EMPTY
                    return Transaction.openRoot().use {
                        val result = handle.extract(index, ItemResource.of(target.item), count, it)
                        ItemStack(target.item, result)
                    }
                }
                return ItemStack.EMPTY
            }

            override fun storeActual(handle: ResourceHandler<ItemResource>, store: ItemStack): ItemStack {
                if (handle.isInRange(index)) {
                    return Transaction.openRoot().use {
                        val result = handle.insert(index, ItemResource.of(store.item), store.count, it)
                        it.commit()
                        ItemStack(store.item, store.count - result)
                    }
                }
                return store
            }

            override fun storeTest(handle: ResourceHandler<ItemResource>, store: ItemStack): ItemStack {
                if (handle.isInRange(index)) {
                    return Transaction.openRoot().use {
                        val result = handle.insert(index, ItemResource.of(store.item), store.count, it)
                        ItemStack(store.item, store.count - result)
                    }
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

        /**
         * @property itemStack operate target, [ItemStack.count] is used as [count]
         */
        data class ByItem(val itemStack: ItemStack) : Item {
            override var count: Int
                get() = itemStack.count
                set(value) {
                    itemStack.count = value
                }

            override fun extractActual(handle: ResourceHandler<ItemResource>): ItemStack {
                val itemReturn = itemStack.copyWithCount(0)
                for (index in 0..<handle.size()) {
                    if (itemReturn.count < itemStack.count) {
                        if (ItemStack.isSameItemSameComponents(itemStack, ItemUtil.getStack(handle, index))) {
                            Transaction.openRoot().use {
                                itemReturn.count += handle.extract(index, ItemResource.of(itemStack), itemStack.count - itemReturn.count, it)
                                it.commit()
                            }
                        }
                    } else break
                }
                return itemReturn
            }

            override fun extractTest(handle: ResourceHandler<ItemResource>): ItemStack {
                val itemReturn = itemStack.copyWithCount(0)
                for (index in 0..<handle.size()) {
                    if (itemReturn.count < itemStack.count) {
                        if (ItemStack.isSameItemSameComponents(itemStack, ItemUtil.getStack(handle, index))) {
                            Transaction.openRoot().use {
                                itemReturn.count += handle.extract(index, ItemResource.of(itemStack), itemStack.count - itemReturn.count, it)
                            }
                        }
                    } else break
                }
                return itemReturn
            }

            override fun storeActual(handle: ResourceHandler<ItemResource>, store: ItemStack): ItemStack {
                val remain = store.copy()
                for (index in 0..<handle.size()) {
                    if (remain.count > 0) {
                        Transaction.openRoot().use {
                            remain.count -= handle.insert(index, ItemResource.of(remain), remain.count, it)
                            it.commit()
                        }
                    } else break
                }
                return remain
            }

            override fun storeTest(handle: ResourceHandler<ItemResource>, store: ItemStack): ItemStack {
                val remain = store.copy()
                for (index in 0..<handle.size()) {
                    if (remain.count > 0) {
                        Transaction.openRoot().use {
                            remain.count -= handle.insert(index, ItemResource.of(remain), remain.count, it)
                        }
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

                override fun encode(buffer: RegistryFriendlyByteBuf, value: Item) {
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

    sealed interface Energy : TransPath<EnergyHandler, Int> {

        override fun isEmpty(obj: Int): Boolean = obj <= 0

        override fun getCount(obj: Int): Int = obj

        class ByAmount(override var count: Int) : Energy {
            override fun extractActual(handle: EnergyHandler): Int = Transaction.openRoot().use {
                val ret = handle.extract(count, it)
                it.commit()
                ret
            }
            override fun extractTest(handle: EnergyHandler): Int = Transaction.openRoot().use {
                handle.extract(count, it)
            }
            override fun storeActual(handle: EnergyHandler, store: Int): Int = Transaction.openRoot().use {
                val ret = count - handle.insert(count, it)
                it.commit()
                ret
            }
            override fun storeTest(handle: EnergyHandler, store: Int): Int = Transaction.openRoot().use {
                count - handle.insert(count, it)
            }
        }

        companion object {
            val STREAM_CODEC: StreamCodec<FriendlyByteBuf, Energy> = object : StreamCodec<FriendlyByteBuf, Energy> {
                override fun decode(buffer: FriendlyByteBuf): Energy {
                    return ByAmount(buffer.readVarInt())
                }

                override fun encode(buffer: FriendlyByteBuf, value: Energy) {
                    buffer.writeVarInt((value as ByAmount).count)
                }
            }
        }
    }

    sealed interface Fluid : TransPath<ResourceHandler<FluidResource>, FluidStack> {
        override fun getCount(obj: FluidStack): Int = obj.amount

        override fun isEmpty(obj: FluidStack): Boolean = obj.isEmpty

        /**
         * @property index the target tank in [ResourceHandler]
         * @property stack operate target, [FluidStack.amount] implementation [count]
         */
        class ByIndex(val index: Int, val stack: FluidStack) : Fluid {
            override var count: Int
                get() = stack.amount
                set(value) {
                    stack.amount = value
                }

            override fun extractActual(handle: ResourceHandler<FluidResource>): FluidStack {// TODO: sus code, figure out its usage
                if (index in 0..<handle.size()) {
                    if (stack.isEmpty) return FluidStack.EMPTY
                    if (FluidStack.isSameFluidSameComponents(stack, FluidUtil.getStack(handle, index))) return FluidStack.EMPTY
                    return Transaction.openRoot().use {
                        val ret = handle.extract(index, FluidResource.of(stack), stack.amount, it)
                        it.commit()
                        FluidStack(stack.fluid, ret)
                    }
                } else return FluidStack.EMPTY
            }

            override fun extractTest(handle: ResourceHandler<FluidResource>): FluidStack {
                if (index in 0..<handle.size()) {
                    if (stack.isEmpty) return FluidStack.EMPTY
                    if (FluidStack.isSameFluidSameComponents(stack, FluidUtil.getStack(handle, index))) return FluidStack.EMPTY
                    return Transaction.openRoot().use {
                        val ret = handle.extract(index, FluidResource.of(stack), stack.amount, it)
                        FluidStack(stack.fluid, ret)
                    }
                } else return FluidStack.EMPTY
            }

            override fun storeActual(handle: ResourceHandler<FluidResource>, store: FluidStack): FluidStack {
                if (index in 0..<handle.size()) {
                    if (FluidStack.isSameFluidSameComponents(stack, FluidUtil.getStack(handle, index))) return FluidStack.EMPTY
                    return Transaction.openRoot().use {
                        val filledCount = handle.insert(index, FluidResource.of(stack), stack.amount, it)
                        it.commit()
                        stack.amount -= filledCount
                        stack
                    }
                } else return FluidStack.EMPTY
            }

            override fun storeTest(handle: ResourceHandler<FluidResource>, store: FluidStack): FluidStack {
                if (index in 0..<handle.size()) {
                    if (FluidStack.isSameFluidSameComponents(stack, FluidUtil.getStack(handle, index))) return FluidStack.EMPTY
                    return Transaction.openRoot().use {
                        val filledCount = handle.insert(index, FluidResource.of(stack), stack.amount, it)
                        stack.amount -= filledCount
                        stack
                    }
                } else return FluidStack.EMPTY
            }

            companion object {
                val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ByIndex> = object : StreamCodec<RegistryFriendlyByteBuf, ByIndex> {
                    override fun decode(buffer: RegistryFriendlyByteBuf): ByIndex {
                        val index = buffer.readVarInt()
                        val stack = FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer)
                        return ByIndex(index, stack)
                    }

                    override fun encode(
                        buffer: RegistryFriendlyByteBuf,
                        value: ByIndex
                    ) {
                        buffer.writeVarInt(value.index)
                        FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, value.stack)
                    }
                }
            }
        }

        companion object {
            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Fluid> = object : StreamCodec<RegistryFriendlyByteBuf, Fluid> {
                override fun decode(buffer: RegistryFriendlyByteBuf): Fluid {
                    TODO("Not yet implemented")
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf,
                    value: Fluid
                ) {
                    TODO("Not yet implemented")
                }
            }
        }
    }

    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TransPath<*, *>> = object : StreamCodec<RegistryFriendlyByteBuf, TransPath<*, *>> {
            override fun decode(buffer: RegistryFriendlyByteBuf): TransPath<*, *> {
                return when (buffer.readShort().toInt()) {
                    0 -> Item.STREAM_CODEC.decode(buffer)
                    1 -> Fluid.STREAM_CODEC.decode(buffer)
                    2 -> Energy.STREAM_CODEC.decode(buffer)
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

                    is Fluid.ByIndex -> {
                        buffer.writeShort(0)
                        Fluid.STREAM_CODEC.encode(buffer, value)
                    }

                    is Energy.ByAmount -> {
                        buffer.writeShort(2)
                        Energy.STREAM_CODEC.encode(buffer, value)
                    }
                }
            }
        }
    }
}
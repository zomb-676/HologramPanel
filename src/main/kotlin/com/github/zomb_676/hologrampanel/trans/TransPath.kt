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

/**
 * query/store actual/test object in handles
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

    sealed interface Item : TransPath<IItemHandler, ItemStack> {

        override fun getCount(obj: ItemStack): Int = obj.count

        override fun isEmpty(obj: ItemStack): Boolean = obj.isEmpty

        fun IItemHandler.isInRange(index: Int) = index >= 0 && index < count

        /**
         * operate items at specific slot and
         *
         * @property index slot index
         * @property count operate count
         * @property target work as a filter, use [ItemStack.isSameItemSameComponents]
         */
        data class ByIndex(val index: Int, override var count: Int, val target: ItemStack = ItemStack.EMPTY) : Item {
            override fun extractActual(handle: IItemHandler): ItemStack {
                if (handle.isInRange(index)) {
                    if (!target.isEmpty && !ItemStack.isSameItemSameComponents(target, handle.getStackInSlot(index))) return ItemStack.EMPTY
                    return handle.extractItem(index, count, false)
                }
                return ItemStack.EMPTY
            }

            override fun extractTest(handle: IItemHandler): ItemStack {
                if (handle.isInRange(index)) {
                    if (!target.isEmpty && !ItemStack.isSameItemSameComponents(target, handle.getStackInSlot(index))) return ItemStack.EMPTY
                    return handle.extractItem(index, count, true)
                }
                return ItemStack.EMPTY
            }

            override fun storeActual(handle: IItemHandler, store: ItemStack): ItemStack {
                if (handle.isInRange(index)) {
                    return handle.insertItem(index, store, false)
                }
                return store
            }

            override fun storeTest(handle: IItemHandler, store: ItemStack): ItemStack {
                if (handle.isInRange(index)) {
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

        /**
         * @property itemStack operate target, [ItemStack.count] is used as [count]
         */
        data class ByItem(val itemStack: ItemStack) : Item {
            override var count: Int
                get() = itemStack.count
                set(value) {
                    itemStack.count = value
                }

            override fun extractActual(handle: IItemHandler): ItemStack {
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

            override fun extractTest(handle: IItemHandler): ItemStack {
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

            override fun storeActual(handle: IItemHandler, store: ItemStack): ItemStack {
                var remain = store
                for (index in 0..<handle.slots) {
                    if (remain.count > 0) {
                        remain = handle.insertItem(index, remain, false)
                    } else break
                }
                return remain
            }

            override fun storeTest(handle: IItemHandler, store: ItemStack): ItemStack {
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

    sealed interface Energy : TransPath<IEnergyStorage, Int> {

        override fun isEmpty(obj: Int): Boolean = obj <= 0

        override fun getCount(obj: Int): Int = obj

        class ByAmount(override var count: Int) : Energy {
            override fun extractActual(handle: IEnergyStorage): Int = handle.extractEnergy(count, false)
            override fun extractTest(handle: IEnergyStorage): Int = handle.extractEnergy(count, true)
            override fun storeActual(handle: IEnergyStorage, store: Int): Int = count - handle.receiveEnergy(count, false)
            override fun storeTest(handle: IEnergyStorage, store: Int): Int = count - handle.receiveEnergy(count, false)
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

    sealed interface Fluid : TransPath<IFluidHandler, FluidStack> {
        override fun getCount(obj: FluidStack): Int = obj.amount

        override fun isEmpty(obj: FluidStack): Boolean = obj.isEmpty

        /**
         * @property index the target tank in [IFluidHandler]
         * @property stack operate target, [FluidStack.amount] implementation [count]
         */
        class ByIndex(val index: Int, val stack: FluidStack) : Fluid {
            override var count: Int
                get() = stack.amount
                set(value) {
                    stack.amount = value
                }

            override fun extractActual(handle: IFluidHandler): FluidStack {
                if (index in 0..<handle.tanks) {
                    if (stack.isEmpty) return FluidStack.EMPTY
                    if (FluidStack.isSameFluidSameComponents(stack, handle.getFluidInTank(index))) return FluidStack.EMPTY
                    return handle.drain(stack, IFluidHandler.FluidAction.EXECUTE)
                } else return FluidStack.EMPTY
            }

            override fun extractTest(handle: IFluidHandler): FluidStack {
                if (index in 0..<handle.tanks) {
                    if (stack.isEmpty) return FluidStack.EMPTY
                    if (FluidStack.isSameFluidSameComponents(stack, handle.getFluidInTank(index))) return FluidStack.EMPTY
                    return handle.drain(stack, IFluidHandler.FluidAction.SIMULATE)
                } else return FluidStack.EMPTY
            }

            override fun storeActual(handle: IFluidHandler, store: FluidStack): FluidStack {
                if (index in 0..<handle.tanks) {
                    if (FluidStack.isSameFluidSameComponents(stack, handle.getFluidInTank(index))) return FluidStack.EMPTY
                    val filledCount = handle.fill(stack, IFluidHandler.FluidAction.EXECUTE)
                    stack.amount -= filledCount
                    return stack
                } else return FluidStack.EMPTY
            }

            override fun storeTest(handle: IFluidHandler, store: FluidStack): FluidStack {
                if (index in 0..<handle.tanks) {
                    if (FluidStack.isSameFluidSameComponents(stack, handle.getFluidInTank(index))) return FluidStack.EMPTY
                    val filledCount = handle.fill(stack, IFluidHandler.FluidAction.SIMULATE)
                    stack.amount -= filledCount
                    return stack
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
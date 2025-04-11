package com.github.zomb_676.hologrampanel.trans

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
sealed interface TransTarget<S : Any, H : Any, R : Any> {
    fun getHandle(source: S): H?

    fun queryActual(source: S, path: TransPath<H, R>): R? = getHandle(source)?.run(path::extractActual)
    fun queryTest(source: S, path: TransPath<H, R>): R? = getHandle(source)?.run(path::extractTest)

    fun storeActual(source: S, path: TransPath<H, R>, store: R): R? {
        val handle = getHandle(source) ?: return store
        return path.storeActual(handle, store)
    }

    fun storeTest(source: S, path: TransPath<H, R>, store: R): R? {
        val handle = getHandle(source) ?: return store
        return path.storeTest(handle, store)
    }

    object EntityItemTarget : TransTarget<Entity, IItemHandler, ItemStack> {
        override fun getHandle(source: Entity): IItemHandler? = source.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, null)
    }

    object BlockItemTarget : TransTarget<BlockEntity, IItemHandler, ItemStack> {
        override fun getHandle(source: BlockEntity): IItemHandler? {
            val level = source.level ?: return null
            return level.getCapability(Capabilities.ItemHandler.BLOCK, source.blockPos, source.blockState, source, null)
        }
    }

    object ItemItemTarget : TransTarget<ItemStack, IItemHandler, ItemStack> {
        override fun getHandle(source: ItemStack): IItemHandler? = source.getCapability(Capabilities.ItemHandler.ITEM)
    }

    object EntityFluidTarget : TransTarget<Entity, IFluidHandler, FluidStack> {
        override fun getHandle(source: Entity): IFluidHandler? = source.getCapability(Capabilities.FluidHandler.ENTITY, null)
    }

    object BlockFluidTarget : TransTarget<BlockEntity, IFluidHandler, FluidStack> {
        override fun getHandle(source: BlockEntity): IFluidHandler? {
            val level = source.level ?: return null
            return level.getCapability(Capabilities.FluidHandler.BLOCK, source.blockPos, source.blockState, source, null)
        }
    }

    object ItemFluidTarget : TransTarget<ItemStack, IFluidHandler, FluidStack> {
        override fun getHandle(source: ItemStack): IFluidHandler? = source.getCapability(Capabilities.FluidHandler.ITEM)
    }

    object EntityEnergyTarget : TransTarget<Entity, IEnergyStorage, Int> {
        override fun getHandle(source: Entity): IEnergyStorage? = source.getCapability(Capabilities.EnergyStorage.ENTITY, null)
    }

    object BlockEnergyTarget : TransTarget<BlockEntity, IEnergyStorage, Int> {
        override fun getHandle(source: BlockEntity): IEnergyStorage? {
            val level = source.level ?: return null
            return level.getCapability(Capabilities.EnergyStorage.BLOCK, source.blockPos, source.blockState, source, null)
        }
    }

    object ItemEnergyTarget : TransTarget<ItemStack, IEnergyStorage, Int> {
        override fun getHandle(source: ItemStack): IEnergyStorage? = source.getCapability(Capabilities.EnergyStorage.ITEM)
    }

    class ChainTarget<S : Any, H : Any, R : Any, H2 : Any, R2 : Any>(
        val before: TransTarget<S, H, R>,
        val path: TransPath<H, R>,
        val transTarget: TransTarget<R, H2, R2>
    ) : TransTarget<S, H2, R2> {
        override fun getHandle(source: S): H2? {
            val r = before.queryTest(source, path) ?: return null
            return transTarget.getHandle(r)
        }
    }

    fun <H2 : Any, R2 : Any> then(path: TransPath<H, R>, transTarget: TransTarget<R, H2, R2>): TransTarget<S, H2, R2> =
        ChainTarget(this, path, transTarget)

    object TransTargetStreamCodec : StreamCodec<RegistryFriendlyByteBuf, TransTarget<*, *, *>> {
        private enum class TargetType {
            ENTITY_ITEM,
            BLOCK_ITEM,
            ITEM_ITEM,
            ENTITY_FLUID,
            BLOCK_FLUID,
            ITEM_FLUID,
            ENTITY_ENERGY,
            BLOCK_ENERGY,
            ITEM_ENERGY,
            CHAIN
        }

        override fun decode(buf: RegistryFriendlyByteBuf): TransTarget<*, *, *> {
            val type = buf.readEnum(TargetType::class.java)
            return when (type) {
                TargetType.ENTITY_ITEM -> EntityItemTarget
                TargetType.BLOCK_ITEM -> BlockItemTarget
                TargetType.ITEM_ITEM -> ItemItemTarget
                TargetType.ENTITY_FLUID -> EntityFluidTarget
                TargetType.BLOCK_FLUID -> BlockFluidTarget
                TargetType.ITEM_FLUID -> ItemFluidTarget
                TargetType.ENTITY_ENERGY -> EntityEnergyTarget
                TargetType.BLOCK_ENERGY -> BlockEnergyTarget
                TargetType.ITEM_ENERGY -> ItemEnergyTarget
                TargetType.CHAIN -> decodeChainTarget(buf)
            }
        }

        override fun encode(buf: RegistryFriendlyByteBuf, value: TransTarget<*, *, *>) {
            when (value) {
                EntityItemTarget -> buf.writeEnum(TargetType.ENTITY_ITEM)
                BlockItemTarget -> buf.writeEnum(TargetType.BLOCK_ITEM)
                ItemItemTarget -> buf.writeEnum(TargetType.ITEM_ITEM)
                EntityFluidTarget -> buf.writeEnum(TargetType.ENTITY_FLUID)
                BlockFluidTarget -> buf.writeEnum(TargetType.BLOCK_FLUID)
                ItemFluidTarget -> buf.writeEnum(TargetType.ITEM_FLUID)
                EntityEnergyTarget -> buf.writeEnum(TargetType.ENTITY_ENERGY)
                BlockEnergyTarget -> buf.writeEnum(TargetType.BLOCK_ENERGY)
                ItemEnergyTarget -> buf.writeEnum(TargetType.ITEM_ENERGY)
                is ChainTarget<*, *, *, *, *> -> encodeChainTarget(buf, value)
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun decodeChainTarget(buf: RegistryFriendlyByteBuf): TransTarget<*, *, *> {
            val before = decode(buf)
            val path = TransPath.STREAM_CODEC.decode(buf)
            val transTarget = decode(buf)
            return ChainTarget(
                before as TransTarget<Any, Any, Any>,
                path as TransPath<Any, Any>,
                transTarget as TransTarget<Any, Any, Any>
            )
        }

        private fun encodeChainTarget(buf: RegistryFriendlyByteBuf, chain: ChainTarget<*, *, *, *, *>) {
            buf.writeEnum(TargetType.CHAIN)
            encode(buf, chain.before)
            TransPath.STREAM_CODEC.encode(buf, chain.path)
            encode(buf, chain.transTarget)
        }
    }
}
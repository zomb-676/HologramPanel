package com.github.zomb_676.hologrampanel.trans

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.access.ItemAccess
import net.neoforged.neoforge.transfer.energy.EnergyHandler
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.item.ItemResource

/**
 * get the handle form [TransSource], like getCapability
 *
 * @param S source type, same in [TransSource]
 * @param H handle type, same in [TransPath]
 */
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
sealed interface TransHandle<in S : Any, out H : Any> {
    fun getHandle(source: S): H?
    fun getHandleNullable(source: S?): H? = if (source != null) getHandle(source) else null

    fun hasHandle(source: S): Boolean = getHandle(source) != null
    fun hasHandleNullable(source: S?): Boolean = source != null && hasHandle(source)

    fun <R : Any> queryActual(source: S, path: TransPath<H, R>): R? = getHandle(source)?.run(path::extractActual)
    fun <R : Any> queryTest(source: S, path: TransPath<H, R>): R? = getHandle(source)?.run(path::extractTest)

    fun <R : Any> storeActual(source: S, path: TransPath<H, R>, store: R): R? {
        val handle = getHandle(source) ?: return store
        return path.storeActual(handle, store)
    }

    fun <R : Any> storeTest(source: S, path: TransPath<H, R>, store: R): R? {
        val handle = getHandle(source) ?: return store
        return path.storeTest(handle, store)
    }

    object EntityItemTransHandle : TransHandle<Entity, ResourceHandler<ItemResource>> {
        override fun getHandle(source: Entity): ResourceHandler<ItemResource>? = source.getCapability(Capabilities.Item.ENTITY_AUTOMATION, null)
    }

    object BlockItemTransHandle : TransHandle<BlockEntity, ResourceHandler<ItemResource>> {
        override fun getHandle(source: BlockEntity): ResourceHandler<ItemResource>? {
            val level = source.level ?: return null
            return level.getCapability(Capabilities.Item.BLOCK, source.blockPos, source.blockState, source, null)
        }
    }

    object ItemItemTransHandle : TransHandle<ItemStack, ResourceHandler<ItemResource>> {
        override fun getHandle(source: ItemStack): ResourceHandler<ItemResource>? = ItemAccess.forStack(source).getCapability(Capabilities.Item.ITEM)
    }

    object EntityFluidTransHandle : TransHandle<Entity, ResourceHandler<FluidResource>> {
        override fun getHandle(source: Entity): ResourceHandler<FluidResource>? = source.getCapability(Capabilities.Fluid.ENTITY, null)
    }

    object BlockFluidTransHandle : TransHandle<BlockEntity, ResourceHandler<FluidResource>> {
        override fun getHandle(source: BlockEntity): ResourceHandler<FluidResource>? {
            val level = source.level ?: return null
            return level.getCapability(Capabilities.Fluid.BLOCK, source.blockPos, source.blockState, source, null)
        }
    }

    object ItemFluidTransHandle : TransHandle<ItemStack, ResourceHandler<FluidResource>> {
        override fun getHandle(source: ItemStack): ResourceHandler<FluidResource>? = ItemAccess.forStack(source).getCapability(Capabilities.Fluid.ITEM)
    }

    object EntityEnergyTransHandle : TransHandle<Entity, EnergyHandler> {
        override fun getHandle(source: Entity): EnergyHandler? = source.getCapability(Capabilities.Energy.ENTITY, null)
    }

    object BlockEnergyTransHandle : TransHandle<BlockEntity, EnergyHandler> {
        override fun getHandle(source: BlockEntity): EnergyHandler? {
            val level = source.level ?: return null
            return level.getCapability(Capabilities.Energy.BLOCK, source.blockPos, source.blockState, source, null)
        }
    }

    object ItemEnergyTransHandle : TransHandle<ItemStack, EnergyHandler> {
        override fun getHandle(source: ItemStack): EnergyHandler? = ItemAccess.forStack(source).getCapability(Capabilities.Energy.ITEM)
    }

    class ChainTransHandle<S : Any, H : Any, R : Any, H2 : Any>(
        val before: TransHandle<S, H>,
        val path: TransPath<H, R>,
        val transHandle: TransHandle<R, H2>
    ) : TransHandle<S, H2> {
        override fun getHandle(source: S): H2? {
            val r = before.queryTest(source, path) ?: return null
            return transHandle.getHandle(r)
        }
    }

    fun <R : Any, H2 : Any> then(path: TransPath<H, R>, transHandle: TransHandle<R, H2>): TransHandle<S, H2> =
        ChainTransHandle(this, path, transHandle)

    object TransTargetStreamCodec : StreamCodec<RegistryFriendlyByteBuf, TransHandle<*, *>> {
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

        override fun decode(buf: RegistryFriendlyByteBuf): TransHandle<*, *> {
            val type = buf.readEnum(TargetType::class.java)
            return when (type) {
                TargetType.ENTITY_ITEM -> EntityItemTransHandle
                TargetType.BLOCK_ITEM -> BlockItemTransHandle
                TargetType.ITEM_ITEM -> ItemItemTransHandle
                TargetType.ENTITY_FLUID -> EntityFluidTransHandle
                TargetType.BLOCK_FLUID -> BlockFluidTransHandle
                TargetType.ITEM_FLUID -> ItemFluidTransHandle
                TargetType.ENTITY_ENERGY -> EntityEnergyTransHandle
                TargetType.BLOCK_ENERGY -> BlockEnergyTransHandle
                TargetType.ITEM_ENERGY -> ItemEnergyTransHandle
                TargetType.CHAIN -> decodeChainTarget(buf)
            }
        }

        override fun encode(buf: RegistryFriendlyByteBuf, value: TransHandle<*, *>) {
            when (value) {
                EntityItemTransHandle -> buf.writeEnum(TargetType.ENTITY_ITEM)
                BlockItemTransHandle -> buf.writeEnum(TargetType.BLOCK_ITEM)
                ItemItemTransHandle -> buf.writeEnum(TargetType.ITEM_ITEM)
                EntityFluidTransHandle -> buf.writeEnum(TargetType.ENTITY_FLUID)
                BlockFluidTransHandle -> buf.writeEnum(TargetType.BLOCK_FLUID)
                ItemFluidTransHandle -> buf.writeEnum(TargetType.ITEM_FLUID)
                EntityEnergyTransHandle -> buf.writeEnum(TargetType.ENTITY_ENERGY)
                BlockEnergyTransHandle -> buf.writeEnum(TargetType.BLOCK_ENERGY)
                ItemEnergyTransHandle -> buf.writeEnum(TargetType.ITEM_ENERGY)
                is ChainTransHandle<*, *, *, *> -> encodeChainTarget(buf, value)
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun decodeChainTarget(buf: RegistryFriendlyByteBuf): TransHandle<*, *> {
            val before = decode(buf)
            val path = TransPath.STREAM_CODEC.decode(buf)
            val transTarget = decode(buf)
            return ChainTransHandle(
                before as TransHandle<Any, Any>,
                path as TransPath<Any, Any>,
                transTarget as TransHandle<Any, Any>
            )
        }

        private fun encodeChainTarget(buf: RegistryFriendlyByteBuf, chain: ChainTransHandle<*, *, *, *>) {
            buf.writeEnum(TargetType.CHAIN)
            encode(buf, chain.before)
            TransPath.STREAM_CODEC.encode(buf, chain.path)
            encode(buf, chain.transHandle)
        }
    }
}
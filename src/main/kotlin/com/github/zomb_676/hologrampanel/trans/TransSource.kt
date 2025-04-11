package com.github.zomb_676.hologrampanel.trans

import com.github.zomb_676.hologrampanel.AllRegisters
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.server.ServerLifecycleHooks

sealed interface TransSource<T> {
    fun getTarget(): T?

    class BlockEntitySource(val pos: BlockPos, val level: ResourceKey<Level>) : TransSource<BlockEntity> {
        override fun getTarget(): BlockEntity? = ServerLifecycleHooks.getCurrentServer()?.getLevel(level)?.getBlockEntity(pos)

        companion object {
            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BlockEntitySource> = StreamCodec.composite(
                BlockPos.STREAM_CODEC, BlockEntitySource::pos, AllRegisters.Codecs.LEVEL_STREAM_CODE, BlockEntitySource::level, ::BlockEntitySource
            )
        }
    }

    class EntitySource(val id: Int, val level: ResourceKey<Level>) : TransSource<Entity> {
        override fun getTarget(): Entity? = ServerLifecycleHooks.getCurrentServer()?.getLevel(level)?.getEntity(id)

        companion object {
            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, EntitySource> = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, EntitySource::id, AllRegisters.Codecs.LEVEL_STREAM_CODE, EntitySource::level, ::EntitySource
            )
        }
    }

    companion object {
        val STREAM_CODEC = object : StreamCodec<RegistryFriendlyByteBuf, TransSource<*>> {
            override fun decode(buffer: RegistryFriendlyByteBuf): TransSource<*> = when (buffer.readShort().toInt()) {
                0 -> BlockEntitySource.STREAM_CODEC.decode(buffer)
                1 -> EntitySource.STREAM_CODEC.decode(buffer)
                else -> throw RuntimeException()
            }

            override fun encode(buffer: RegistryFriendlyByteBuf, value: TransSource<*>) = when (value) {
                is BlockEntitySource -> {
                    buffer.writeShort(0)
                    BlockEntitySource.STREAM_CODEC.encode(buffer, value)
                }

                is EntitySource -> {
                    buffer.writeShort(1)
                    EntitySource.STREAM_CODEC.encode(buffer, value)
                }
            }
        }

        fun create(entity: Entity) = EntitySource(entity.id, entity.level().dimension())
        fun create(blockEntity: BlockEntity) = BlockEntitySource(blockEntity.blockPos, blockEntity.level!!.dimension())
    }
}
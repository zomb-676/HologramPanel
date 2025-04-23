package com.github.zomb_676.hologrampanel.trans

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.server.ServerLifecycleHooks

/**
 * describe the source of the handles, like CapabilityProvider
 *
 * @param T the source type
 */
sealed interface TransSource<out T : Any> {
    fun getTarget(): T?

    class BlockEntitySource(val pos: BlockPos, val level: ResourceKey<Level>) : TransSource<BlockEntity> {
        override fun getTarget(): BlockEntity? = ServerLifecycleHooks.getCurrentServer()?.getLevel(level)?.getBlockEntity(pos)

        companion object {
            val STREAM_CODEC: StreamCodec<FriendlyByteBuf, BlockEntitySource> = StreamCodec.composite(
                ByteBufCodecs.BLOCK_POS, BlockEntitySource::pos, AllRegisters.Codecs.LEVEL_STREAM_CODE, BlockEntitySource::level, ::BlockEntitySource
            )
        }
    }

    class EntitySource(val id: Int, val level: ResourceKey<Level>) : TransSource<Entity> {
        override fun getTarget(): Entity? = ServerLifecycleHooks.getCurrentServer()?.getLevel(level)?.getEntity(id)

        companion object {
            val STREAM_CODEC: StreamCodec<FriendlyByteBuf, EntitySource> = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, EntitySource::id, AllRegisters.Codecs.LEVEL_STREAM_CODE, EntitySource::level, ::EntitySource
            )
        }
    }

    companion object {
        val STREAM_CODEC = object : StreamCodec<FriendlyByteBuf, TransSource<*>> {
            override fun decode(buffer: FriendlyByteBuf): TransSource<*> = when (buffer.readShort().toInt()) {
                0 -> BlockEntitySource.STREAM_CODEC.decode(buffer)
                1 -> EntitySource.STREAM_CODEC.decode(buffer)
                else -> throw RuntimeException()
            }

            override fun encode(buffer: FriendlyByteBuf, value: TransSource<*>) = when (value) {
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
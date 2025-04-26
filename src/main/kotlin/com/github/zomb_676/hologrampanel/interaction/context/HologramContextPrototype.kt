package com.github.zomb_676.hologrampanel.interaction.context

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.util.StringRepresentable
import java.util.*

sealed interface HologramContextPrototype {

    fun prototypeEnum(): PrototypeEnum

    class BlockHologramPrototype(val pos: BlockPos) : HologramContextPrototype {
        fun create(player: LocalPlayer): BlockHologramContext {
            val context = BlockHologramContext(pos, player)
            return context
        }

        override fun prototypeEnum(): PrototypeEnum = PrototypeEnum.BLOCK

        companion object {
            val CODEC: Codec<BlockHologramPrototype> = RecordCodecBuilder.create { ins ->
                ins.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(BlockHologramPrototype::pos)
                ).apply(ins, ::BlockHologramPrototype)
            }

            fun extract(context: BlockHologramContext): BlockHologramPrototype {
                return BlockHologramPrototype(context.pos)
            }
        }
    }

    class EntityHologramPrototype(val entityUUID: UUID) : HologramContextPrototype {
        fun create(player: LocalPlayer): EntityHologramContext? {
            val entity = player.level().getEntities(null, player.boundingBox)
                .firstOrNull { it.uuid == entityUUID } ?: return null
            return EntityHologramContext(entity, player)
        }

        override fun prototypeEnum(): PrototypeEnum = PrototypeEnum.ENTITY

        companion object {
            val CODEC: Codec<EntityHologramPrototype> = RecordCodecBuilder.create { ins ->
                ins.group(
                    UUIDUtil.CODEC.fieldOf("entityUUID").forGetter(EntityHologramPrototype::entityUUID)
                ).apply(ins, ::EntityHologramPrototype)
            }
        }
    }

    enum class PrototypeEnum : StringRepresentable {
        BLOCK {
            override fun codec(): Codec<BlockHologramPrototype> = BlockHologramPrototype.CODEC

        },
        ENTITY {
            override fun codec(): Codec<EntityHologramPrototype> = EntityHologramPrototype.CODEC
        };

        override fun getSerializedName(): String = this.name
        abstract fun codec(): Codec<out HologramContextPrototype>

        companion object {
            val ENUM_CODEC: Codec<PrototypeEnum> = StringRepresentable.fromEnum(PrototypeEnum::values)
        }
    }

    companion object {
        fun extract(context: HologramContext): HologramContextPrototype = when (context) {
            is BlockHologramContext -> BlockHologramPrototype(context.pos)
            is EntityHologramContext -> EntityHologramPrototype(context.getEntity().uuid)
        }

        val CODEC: Codec<HologramContextPrototype> = PrototypeEnum.ENUM_CODEC.dispatch(
            HologramContextPrototype::prototypeEnum,
            PrototypeEnum::codec
        )
    }
}
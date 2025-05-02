package com.github.zomb_676.hologrampanel.interaction.context

import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos

sealed interface HologramContextPrototype {

    class BlockHologramPrototype(val pos: BlockPos) : HologramContextPrototype {
        fun create(player: LocalPlayer): BlockHologramContext {
            val context = BlockHologramContext(pos, player, null)
            return context
        }

        companion object {
            val CODEC = RecordCodecBuilder.create { ins ->
                ins.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(BlockHologramPrototype::pos)
                ).apply(ins, ::BlockHologramPrototype)
            }

            fun extract(context: BlockHologramContext): BlockHologramPrototype {
                return BlockHologramPrototype(context.pos)
            }
        }
    }
}
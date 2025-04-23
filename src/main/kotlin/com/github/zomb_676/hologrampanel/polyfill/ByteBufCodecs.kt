package com.github.zomb_676.hologrampanel.polyfill

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import java.util.*

object ByteBufCodecs {
    interface BufferCodec<I> : StreamCodec<FriendlyByteBuf, I>

    val VAR_INT = object : BufferCodec<Int> {
        override fun decode(buffer: FriendlyByteBuf): Int = buffer.readVarInt()
        override fun encode(buffer: FriendlyByteBuf, value: Int) {
            buffer.writeVarInt(value)
        }
    }

    val INT = object : BufferCodec<Int> {
        override fun decode(buffer: FriendlyByteBuf): Int = buffer.readInt()
        override fun encode(buffer: FriendlyByteBuf, value: Int) {
            buffer.writeInt(value)
        }
    }

    val BOOL = object : BufferCodec<Boolean> {
        override fun decode(buffer: FriendlyByteBuf): Boolean = buffer.readBoolean()
        override fun encode(buffer: FriendlyByteBuf, value: Boolean) {
            buffer.writeBoolean(value)
        }
    }

    val UUID = object : BufferCodec<UUID> {
        override fun decode(buffer: FriendlyByteBuf): UUID = buffer.readUUID()
        override fun encode(buffer: FriendlyByteBuf, value: UUID) {
            buffer.writeUUID(value)
        }
    }

    val ITEM_STACK = object : BufferCodec<ItemStack> {
        override fun decode(buffer: FriendlyByteBuf): ItemStack {
            if (!buffer.readBoolean()) return ItemStack.EMPTY
            @Suppress("DEPRECATION")
            val item = buffer.readById(BuiltInRegistries.ITEM) ?: return ItemStack.EMPTY
            val count = buffer.readVarInt()
            return ItemStack(item, count).apply { readShareTag(buffer.readNbt()) }
        }

        override fun encode(buffer: FriendlyByteBuf, value: ItemStack) {
            if (value.isEmpty) {
                buffer.writeBoolean(false)
                return
            }
            buffer.writeBoolean(true)
            val item = value.item
            @Suppress("DEPRECATION")
            buffer.writeId(BuiltInRegistries.ITEM, item)
            buffer.writeVarInt(value.count)
            val tag = if (item.isDamageable(value) || item.shouldOverrideMultiplayerNbt()) {
                value.tag
            } else null
            buffer.writeNbt(tag)
        }
    }

    val COMPOUND_TAG = object : BufferCodec<CompoundTag> {
        override fun decode(buffer: FriendlyByteBuf): CompoundTag {
            return buffer.readNbt()!!
        }

        override fun encode(buffer: FriendlyByteBuf, value: CompoundTag) {
            buffer.writeNbt(value)
        }
    }

    val BLOCK_POS = object : BufferCodec<BlockPos> {
        override fun decode(buffer: FriendlyByteBuf): BlockPos = buffer.readBlockPos()
        override fun encode(buffer: FriendlyByteBuf, value: BlockPos) {
            buffer.writeBlockPos(value)
        }
    }

    val FLUID_STACK = object : BufferCodec<FluidStack> {
        override fun decode(buffer: FriendlyByteBuf): FluidStack {
            return buffer.readFluidStack()
        }

        override fun encode(buffer: FriendlyByteBuf, value: FluidStack) {
            buffer.writeFluidStack(value)
        }
    }
}
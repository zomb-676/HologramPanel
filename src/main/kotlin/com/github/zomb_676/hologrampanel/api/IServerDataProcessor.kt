package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.widget.component.ContextHolder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity

interface IServerDataProcessor {
    /**
     * this will run on logic server side
     */
    fun appendServerData(context: ContextHolder, buf: RegistryFriendlyByteBuf, level: ServerLevel)

    companion object {
        data object FurnaceData : IServerDataProcessor {
            override fun appendServerData(
                context: ContextHolder,
                buf: RegistryFriendlyByteBuf,
                level: ServerLevel
            ) {
                val pos = context.query(IContextType.BLOCK_POS)
                val blockEntity = level.getBlockEntity(pos) as AbstractFurnaceBlockEntity
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, blockEntity.getItem(0))
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, blockEntity.getItem(1))
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, blockEntity.getItem(2))
            }
        }
    }
}
package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.IPayloadContext
import com.github.zomb_676.hologrampanel.polyfill.IPayloadHandler
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import com.github.zomb_676.hologrampanel.projector.IHologramStorage
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Block
import net.minecraftforge.network.NetworkEvent

class SetProjectorSettingPayload(val nbt: CompoundTag, val pos: BlockPos) : CustomPacketPayload<SetProjectorSettingPayload> {
    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    companion object {
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, SetProjectorSettingPayload> = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SetProjectorSettingPayload::nbt,
            ByteBufCodecs.BLOCK_POS, SetProjectorSettingPayload::pos,
            ::SetProjectorSettingPayload
        )
        val HANDLE = object : IPayloadHandler<SetProjectorSettingPayload> {
            override fun handle(payload: SetProjectorSettingPayload, context: IPayloadContext) {
                val level = (context.player() as ServerPlayer).level() as? ServerLevel? ?: return
                val be = level.getBlockEntity(payload.pos) ?: return
                val storage = be.getCapability(IHologramStorage.CAPABILITY).orElse(null) ?: return
                storage.readFromNbt(payload.nbt)
                be.setChanged()
                level.sendBlockUpdated(payload.pos, be.blockState, be.blockState, Block.UPDATE_CLIENTS)
            }
        }
    }
}
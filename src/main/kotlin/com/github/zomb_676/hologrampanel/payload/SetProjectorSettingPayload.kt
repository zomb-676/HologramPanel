package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.projector.IHologramStorage
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler

class SetProjectorSettingPayload(val nbt: CompoundTag, val pos: BlockPos) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<SetProjectorSettingPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<SetProjectorSettingPayload>(HologramPanel.rl("set_projector_setting"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SetProjectorSettingPayload> = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SetProjectorSettingPayload::nbt,
            BlockPos.STREAM_CODEC, SetProjectorSettingPayload::pos,
            ::SetProjectorSettingPayload
        )
        val HANDLE = object : IPayloadHandler<SetProjectorSettingPayload> {
            override fun handle(payload: SetProjectorSettingPayload, context: IPayloadContext) {
                val level = (context.player() as ServerPlayer).level() as? ServerLevel? ?: return
                val be = level.getBlockEntity(payload.pos) ?: return
                val storage = level.getCapability(IHologramStorage.CAPABILITY, payload.pos) ?: return
                storage.readFromNbt(payload.nbt)
                be.setChanged()
                level.sendBlockUpdated(payload.pos, be.blockState, be.blockState, Block.UPDATE_CLIENTS)
            }
        }
    }

    fun sendToServer() {
        Minecraft.getInstance().player!!.connection.send(this)
    }
}
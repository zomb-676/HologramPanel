package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler

class ServerHandShakePayload(val id : Int = 0) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerHandShakePayload>(HologramPanel.rl("server_hand_shake"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ServerHandShakePayload> = StreamCodec.composite(
            ByteBufCodecs.INT, ServerHandShakePayload::id,
            ::ServerHandShakePayload
        )
        val HANDLE = object : IPayloadHandler<ServerHandShakePayload> {
            override fun handle(
                payload: ServerHandShakePayload,
                context: IPayloadContext
            ) {
                HologramPanel.serverInstalled = true
            }
        }
    }
}
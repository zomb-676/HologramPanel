package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler

/**
 * the payload, which is used when origin payload decoding failed, just makes the decoding system happy
 */
object MimicPayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<MimicPayload> = TYPE

    val TYPE = CustomPacketPayload.Type<MimicPayload>(HologramPanel.rl("mimic_payload"))
    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MimicPayload> = object : StreamCodec<RegistryFriendlyByteBuf, MimicPayload> {
        override fun decode(buffer: RegistryFriendlyByteBuf): MimicPayload {
            return MimicPayload
        }

        override fun encode(buffer: RegistryFriendlyByteBuf, value: MimicPayload) {}
    }
    val HANDLE = object : IPayloadHandler<MimicPayload> {
        override fun handle(payload: MimicPayload, context: IPayloadContext) {
        }
    }
}
package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import java.util.*

class ComponentResponseDataPayload(val uuid: UUID, val data: CompoundTag) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out ComponentResponseDataPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ComponentResponseDataPayload>(HologramPanel.rl("component_response_data"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentResponseDataPayload> = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ComponentResponseDataPayload::uuid,
            ByteBufCodecs.COMPOUND_TAG, ComponentResponseDataPayload::data,
            ::ComponentResponseDataPayload
        )
        val HANDLE = object : IPayloadHandler<ComponentResponseDataPayload> {
            override fun handle(
                payload: ComponentResponseDataPayload,
                context: IPayloadContext
            ) {
                DataQueryManager.Client.receiveData(payload.uuid, payload.data)
            }
        }
    }
}
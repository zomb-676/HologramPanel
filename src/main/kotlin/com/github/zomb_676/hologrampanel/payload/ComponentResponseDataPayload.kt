package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
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

class ComponentResponseDataPayload private constructor(val uuid: UUID, val data: CompoundTag, val sizeInBytes: Int) :
    CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<ComponentResponseDataPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ComponentResponseDataPayload>(HologramPanel.rl("component_response_data"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentResponseDataPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, ComponentResponseDataPayload> {
                override fun decode(buffer: RegistryFriendlyByteBuf): ComponentResponseDataPayload {
                    val uuid = UUIDUtil.STREAM_CODEC.decode(buffer)
                    val begin = buffer.readerIndex()
                    val data = ByteBufCodecs.COMPOUND_TAG.decode(buffer)
                    val size = buffer.readerIndex() - begin
                    return ComponentResponseDataPayload(uuid, data, size)
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf,
                    value: ComponentResponseDataPayload
                ) {
                    UUIDUtil.STREAM_CODEC.encode(buffer, value.uuid)
                    ByteBufCodecs.COMPOUND_TAG.encode(buffer, value.data)
                }

            }
        val HANDLE = object : IPayloadHandler<ComponentResponseDataPayload> {
            override fun handle(
                payload: ComponentResponseDataPayload,
                context: IPayloadContext
            ) {
                DataQueryManager.Client.receiveData(payload.uuid, payload.data, payload.sizeInBytes)
            }
        }

        fun of(uuid: UUID, data: CompoundTag) =
            ComponentResponseDataPayload(uuid, data, 0)
    }
}
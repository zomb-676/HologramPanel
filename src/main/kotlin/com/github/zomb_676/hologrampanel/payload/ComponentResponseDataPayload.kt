package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.polyfill.*
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.google.common.collect.ImmutableMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.*

/**
 * data sync back to the client from server
 *
 * @param uuid the [UUID] identify used during data sync
 * @param data the data synced back to the client
 * @param sizeInBytes ignored under server and only get when decoding at
 * the client, not take space of the actual transforming packets
 */
class ComponentResponseDataPayload private constructor(
    val uuid: UUID,
    val data: ImmutableMap<ComponentProvider<*, *>, CompoundTag>,
    val sizeInBytes: Int
) : CustomPacketPayload<ComponentResponseDataPayload> {

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    companion object {

        val CONTAINER_STREAM_CODEC: StreamCodec<FriendlyByteBuf, ImmutableMap<ComponentProvider<*, *>, CompoundTag>> =
            object : StreamCodec<FriendlyByteBuf, ImmutableMap<ComponentProvider<*, *>, CompoundTag>> {
                override fun decode(buffer: FriendlyByteBuf): ImmutableMap<ComponentProvider<*, *>, CompoundTag> {
                    val size = buffer.readVarInt()
                    val builder: ImmutableMap.Builder<ComponentProvider<*, *>, CompoundTag> = ImmutableMap.builder()
                    repeat(size) { _ ->
                        val id = buffer.readVarInt()
                        val provider = AllRegisters.ComponentHologramProviderRegistry.byId(id)
                        val tag = ByteBufCodecs.COMPOUND_TAG.decode(buffer)
                        builder.put(provider, tag)
                    }
                    return builder.buildOrThrow()
                }

                override fun encode(buffer: FriendlyByteBuf, value: ImmutableMap<ComponentProvider<*, *>, CompoundTag>) {
                    buffer.writeVarInt(value.size)
                    value.forEach { (provider, tag) ->
                        buffer.writeVarInt(AllRegisters.ComponentHologramProviderRegistry.getId(provider))
                        ByteBufCodecs.COMPOUND_TAG.encode(buffer, tag)
                    }
                }

            }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentResponseDataPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, ComponentResponseDataPayload> {
                override fun decode(buffer: RegistryFriendlyByteBuf): ComponentResponseDataPayload {
                    val uuid = ByteBufCodecs.UUID.decode(buffer)
                    val begin = buffer.readerIndex()
                    val data = CONTAINER_STREAM_CODEC.decode(buffer)
                    val size = buffer.readerIndex() - begin
                    return ComponentResponseDataPayload(uuid, data, size)
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf, value: ComponentResponseDataPayload
                ) {
                    ByteBufCodecs.UUID.encode(buffer, value.uuid)
                    CONTAINER_STREAM_CODEC.encode(buffer, value.data)
                }

            }

        val HANDLE = object : IPayloadHandler<ComponentResponseDataPayload> {
            override fun handle(payload: ComponentResponseDataPayload, context: IPayloadContext) {
                DataQueryManager.Client.receiveData(payload.uuid, payload.data, payload.sizeInBytes)
            }
        }

        fun of(uuid: UUID, data: ImmutableMap<ComponentProvider<*, *>, CompoundTag>) = ComponentResponseDataPayload(uuid, data, 0)
    }
}
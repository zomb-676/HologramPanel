package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.IPayloadContext
import com.github.zomb_676.hologrampanel.polyfill.IPayloadHandler
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkEvent
import java.util.*

/**
 * notify the sever to sync data to the client for [ServerDataProvider]
 *
 * @param uuid the [UUID] identify used during data sync
 * @param additionDataTag correspond to [ServerDataProvider.additionInformationForServer]
 * @param providers providers require sync
 * @param context context object for the hologram
 */
class ComponentRequestDataPayload<T : HologramContext>(
    val uuid: UUID, val additionDataTag: CompoundTag, val providers: List<ServerDataProvider<T, *>>, val context: T
) : CustomPacketPayload<ComponentRequestDataPayload<T>>  {

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentRequestDataPayload<*>> =
            object : StreamCodec<RegistryFriendlyByteBuf, ComponentRequestDataPayload<*>> {
                override fun decode(buffer: RegistryFriendlyByteBuf): ComponentRequestDataPayload<*> {
                    val uuid = ByteBufCodecs.UUID.decode(buffer)
                    val additionDataTag = ByteBufCodecs.COMPOUND_TAG.decode(buffer)
                    val providerSize = ByteBufCodecs.VAR_INT.decode(buffer)
                    val providers = List(providerSize) { _ ->
                        AllRegisters.ComponentHologramProviderRegistry.STREAM_CODEC.decode(buffer)
                    }
                    val context = HologramContext.STREAM_CODE.decode(buffer)
                    return ComponentRequestDataPayload(uuid, additionDataTag, providers.unsafeCast(), context)
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf, value: ComponentRequestDataPayload<*>
                ) {
                    ByteBufCodecs.UUID.encode(buffer, value.uuid)
                    ByteBufCodecs.COMPOUND_TAG.encode(buffer, value.additionDataTag)
                    ByteBufCodecs.VAR_INT.encode(buffer, value.providers.size)
                    for (provider in value.providers) {
                        AllRegisters.ComponentHologramProviderRegistry.STREAM_CODEC.encode(
                            buffer, provider as ComponentProvider<*, *>
                        )
                    }
                    HologramContext.STREAM_CODE.encode(buffer, value.context)
                }
            }
        val HANDLE = object : IPayloadHandler<ComponentRequestDataPayload<*>> {
            override fun handle(payload: ComponentRequestDataPayload<*>, context: IPayloadContext) {
                DataQueryManager.Server.create(context.player() as ServerPlayer, payload)
            }
        }
    }
}
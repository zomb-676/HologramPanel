package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.component.ServerDataProvider
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import java.util.*

class ComponentRequestDataPayload<T : HologramContext>(
    val uuid: UUID, val additionDataTag: CompoundTag, val providers: List<ServerDataProvider<T>>, val context: T
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out ComponentRequestDataPayload<*>> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ComponentRequestDataPayload<*>>(HologramPanel.rl("component_request_data"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentRequestDataPayload<*>> =
            object : StreamCodec<RegistryFriendlyByteBuf, ComponentRequestDataPayload<*>> {
                override fun decode(buffer: RegistryFriendlyByteBuf): ComponentRequestDataPayload<*> {
                    val uuid = UUIDUtil.STREAM_CODEC.decode(buffer)
                    val additionDataTag = ByteBufCodecs.COMPOUND_TAG.decode(buffer)
                    val providerSize = ByteBufCodecs.VAR_INT.decode(buffer)
                    val providers = List(providerSize) { _ ->
                        AllRegisters.ComponentHologramProviderRegistry.STREAM_CODEC.decode(buffer)
                    }
                    val context = when (buffer.readShort()) {
                        0.toShort() -> {
                            BlockHologramContext.STREAM_CODEC.decode(buffer)
                        }

                        1.toShort() -> {
                            EntityHologramContext.STREAM_CODE.decode(buffer)
                        }

                        else -> throw RuntimeException()
                    }
                    return ComponentRequestDataPayload(uuid, additionDataTag, providers.unsafeCast(), context)
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf, value: ComponentRequestDataPayload<*>
                ) {
                    UUIDUtil.STREAM_CODEC.encode(buffer, value.uuid)
                    ByteBufCodecs.COMPOUND_TAG.encode(buffer, value.additionDataTag)
                    ByteBufCodecs.VAR_INT.encode(buffer, value.providers.size)
                    for (provider in value.providers) {
                        AllRegisters.ComponentHologramProviderRegistry.STREAM_CODEC.encode(
                            buffer, provider as ComponentProvider<*>
                        )
                    }
                    when (val context = value.context) {
                        is BlockHologramContext -> {
                            buffer.writeShort(0)
                            BlockHologramContext.STREAM_CODEC.encode(buffer, context)
                        }

                        is EntityHologramContext -> {
                            buffer.writeShort(1)
                            EntityHologramContext.STREAM_CODE.encode(buffer, context)
                        }
                    }
                }
            }
        val HANDLE = object : IPayloadHandler<ComponentRequestDataPayload<*>> {
            override fun handle(
                payload: ComponentRequestDataPayload<*>, context: IPayloadContext
            ) {
                DataQueryManager.Server.create(
                    context.player() as ServerPlayer, payload
                )
            }
        }
    }
}
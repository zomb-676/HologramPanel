package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.IServerDataProcessor
import com.github.zomb_676.hologrampanel.api.IServerDataRequester
import com.github.zomb_676.hologrampanel.widget.component.ContextHolder
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidgetRequesterManager
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import java.util.*

class ComponentWidgetQueryPayload(
    val widgetUUID: UUID,
    val context: ContextHolder,
    val requester: List<IServerDataProcessor>
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<ComponentWidgetQueryPayload>(HologramPanel.rl("synchronizer_sync"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentWidgetQueryPayload> = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ComponentWidgetQueryPayload::widgetUUID,
            ContextHolder.STREAM_CODE, ComponentWidgetQueryPayload::context,
            AllRegisters.IServerDataProcessorRegistry.LIST_STREAM_CODEC, ComponentWidgetQueryPayload::requester,
            ::ComponentWidgetQueryPayload
        )
        val HANDLE = object : IPayloadHandler<ComponentWidgetQueryPayload> {
            override fun handle(
                payload: ComponentWidgetQueryPayload,
                context: IPayloadContext
            ) {
                HologramComponentWidgetRequesterManager.Server.createResponse(payload, context)
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
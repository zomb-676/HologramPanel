package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import net.minecraft.client.Minecraft
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import java.util.*

/**
 * notify the sever to stop the sync progress for a specific Hologram by [uuid]
 */
class SyncClosePayload(val uuid: UUID) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<SyncClosePayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<SyncClosePayload>(HologramPanel.rl("sync_close"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncClosePayload> = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SyncClosePayload::uuid, ::SyncClosePayload
        )
        val HANDLE = object : IPayloadHandler<SyncClosePayload> {
            override fun handle(payload: SyncClosePayload, context: IPayloadContext) {
                when (context.flow()) {
                    PacketFlow.SERVERBOUND -> DataQueryManager.Server.closeWidget(
                        context.player().unsafeCast(),
                        payload.uuid
                    )

                    PacketFlow.CLIENTBOUND -> DataQueryManager.Client.closeForWidget(payload.uuid)
                }
            }
        }
    }

    fun sendToServer() {
        Minecraft.getInstance().player!!.connection.send(this)
    }
}
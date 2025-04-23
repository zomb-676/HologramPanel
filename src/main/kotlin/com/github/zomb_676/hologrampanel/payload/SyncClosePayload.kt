package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.IPayloadContext
import com.github.zomb_676.hologrampanel.polyfill.IPayloadHandler
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import io.netty.buffer.ByteBufUtil
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkEvent
import java.util.*

/**
 * notify the sever to stop the sync progress for a specific Hologram by [uuid]
 */
class SyncClosePayload(val uuid: UUID) : CustomPacketPayload<SyncClosePayload> {

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }


    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncClosePayload> = StreamCodec.composite(
            ByteBufCodecs.UUID, SyncClosePayload::uuid, ::SyncClosePayload
        )
        val HANDLE = object : IPayloadHandler<SyncClosePayload> {
            override fun handle(payload: SyncClosePayload, context: IPayloadContext) {
                when (context.flow()) {
                    PacketFlow.SERVERBOUND -> DataQueryManager.Server.closeWidget(
                        context.player() as ServerPlayer,
                        payload.uuid
                    )

                    PacketFlow.CLIENTBOUND -> DataQueryManager.Client.closeForWidget(payload.uuid)
                }
            }
        }
    }
}
package com.github.zomb_676.hologrampanel.sync

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.widget.interactive.DistType
import io.netty.buffer.Unpooled
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import java.util.UUID

class DataSynchronizerSyncPayload(val syncerUUID: UUID, val data: ByteArray) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<DataSynchronizerSyncPayload>(HologramPanel.rl("synchronizer_sync"))
        val STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, DataSynchronizerSyncPayload::syncerUUID,
            NeoForgeStreamCodecs.UNBOUNDED_BYTE_ARRAY, DataSynchronizerSyncPayload::data,
            ::DataSynchronizerSyncPayload
        )

        val HANDLE = object : IPayloadHandler<DataSynchronizerSyncPayload> {
            override fun handle(
                payload: DataSynchronizerSyncPayload,
                context: IPayloadContext
            ) {
                val dist = when (context.connection().direction) {
                    PacketFlow.SERVERBOUND -> DistType.SERVER
                    PacketFlow.CLIENTBOUND -> DistType.CLIENT
                }
                val uuid = payload.syncerUUID
                val syncer = when (dist) {
                    DistType.CLIENT -> SynchronizerManager.Client.syncers[uuid]
                    DistType.SERVER -> SynchronizerManager.Server.syncers[uuid]
                }!!
                val buffer = RegistryFriendlyByteBuf(
                    Unpooled.wrappedBuffer(payload.data),
                    context.player().registryAccess(),
                    context.listener().connectionType
                )
                syncer.onSyncDataReceived(buffer)
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

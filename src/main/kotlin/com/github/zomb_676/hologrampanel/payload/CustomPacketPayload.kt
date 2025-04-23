package com.github.zomb_676.hologrampanel.payload

import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.Vec3
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.PacketDistributor

interface CustomPacketPayload<T> {

    /**
     * @param context [NetworkEvent.Context.packetHandled] will be set
     */
    fun handle(context: NetworkEvent.Context)

    fun sendToServer() = NetworkHandle.sendToServer(this)
    fun send(connection: Connection) = NetworkHandle.send(this, connection)
    private fun sendTo(packetTarget: PacketDistributor.PacketTarget) = NetworkHandle.sendTo(this, packetTarget)
    fun sendToPlayer(player: ServerPlayer) = sendTo(PacketDistributor.PLAYER.with { player })
    fun sendToAll() = sendTo(PacketDistributor.ALL.noArg())
    fun sendToAllInDimension(level: ResourceKey<Level>) = sendTo(PacketDistributor.DIMENSION.with { level })
    fun sendToNear(pos: Vec3, radius: Double, dim: ResourceKey<Level>) = sendTo(
        PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(pos.x, pos.y, pos.z, radius, dim))
    )

    fun sendToTraceChunk(chunk: LevelChunk) = sendTo(PacketDistributor.TRACKING_CHUNK.with { chunk })
    fun sendToTraceChunk(pos: ChunkPos, level: Level): Boolean {
        if (level.hasChunk(pos.x, pos.z)) {
            sendToTraceChunk(level.getChunk(pos.x, pos.z))
            return true
        }
        return false
    }

    fun sendToTraceEntityWithoutSelf(entity: Entity) = sendTo(PacketDistributor.TRACKING_ENTITY.with { entity })
    fun sendToTraceEntityWithSelf(entity: Entity) = sendTo(PacketDistributor.TRACKING_ENTITY_AND_SELF.with { entity })
}
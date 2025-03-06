package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.widget.interactive.DistType
import io.netty.buffer.ByteBuf
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.joml.Vector3fc

class EntityHologramContext(
    val entity: Entity,
    private val player: Player,
    private val hitResult: EntityHitResult?
) : HologramContext {
    internal val tag = CompoundTag()

    override fun getLevel(): Level = player.level()

    override fun getPlayer(): Player = player

    override fun getLogicSide(): DistType = DistType.from(getLevel())

    override fun hologramCenterPosition(): Vector3fc =
        Vector3f(entity.x.toFloat(), entity.y.toFloat(), entity.z.toFloat())

    override fun getIdentityObject(): Any = entity.uuid

    override fun getHitContext(): HitResult? = hitResult

    override fun attachedServerData(): CompoundTag = tag

    companion object {
        fun of(hit: EntityHitResult, player: Player): EntityHologramContext {
            val entity: Entity = hit.entity
            return EntityHologramContext(entity, player, hit)
        }

        private val LEVEL_STREAM_CODE: StreamCodec<ByteBuf, ResourceKey<Level>> =
            ResourceKey.streamCodec(Registries.DIMENSION)
    }
}
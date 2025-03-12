package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.util.DistType
import com.github.zomb_676.hologrampanel.widget.dynamic.Remember
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.server.ServerLifecycleHooks
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3f
import org.joml.Vector3fc
import java.util.*
import kotlin.jvm.optionals.getOrNull

class EntityHologramContext(
    val entity: Entity, private val player: Player, private val hitResult: EntityHitResult?
) : HologramContext {
    private var tag: CompoundTag? = null
    private var remember = Remember.create(this)

    override fun getLevel(): Level = player.level()

    override fun getPlayer(): Player = player

    override fun getLogicSide(): DistType = DistType.from(getLevel())

    override fun hologramCenterPosition(): Vector3fc =
        Vector3f(entity.x.toFloat(), entity.y.toFloat() + (entity.bbHeight), entity.z.toFloat())

    override fun getIdentityObject(): Any = entity.uuid

    override fun getHitContext(): HitResult? = hitResult

    @ApiStatus.Internal
    override fun attachedServerData(): CompoundTag? = tag

    @ApiStatus.Internal
    override fun setServerUpdateDat(tag: CompoundTag) {
        this.tag = tag
    }

    override fun getRememberData(): Remember<out HologramContext> = remember

    companion object {
        fun of(hit: EntityHitResult, player: Player): EntityHologramContext {
            val entity: Entity = hit.entity
            return EntityHologramContext(entity, player, hit)
        }

        val STREAM_CODE: StreamCodec<RegistryFriendlyByteBuf, EntityHologramContext> =
            object : StreamCodec<RegistryFriendlyByteBuf, EntityHologramContext> {
                override fun decode(buffer: RegistryFriendlyByteBuf): EntityHologramContext {
                    val levelKey = AllRegisters.Codecs.LEVEL_STREAM_CODE.decode(buffer)
                    val server = ServerLifecycleHooks.getCurrentServer()!!
                    val level = server.getLevel(levelKey)!!
                    val entity = level.getEntity(buffer.readVarInt())!!
                    val player = server.playerList.getPlayer(UUIDUtil.STREAM_CODEC.decode(buffer))!!
                    val location = buffer.readOptional(Vec3.STREAM_CODEC).getOrNull()
                    val hit = if (location != null) {
                        EntityHitResult(entity, location)
                    } else null
                    return EntityHologramContext(entity, player, hit)
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf, value: EntityHologramContext
                ) {
                    AllRegisters.Codecs.LEVEL_STREAM_CODE.encode(buffer, value.entity.level().dimension())
                    buffer.writeVarInt(value.entity.id)
                    UUIDUtil.STREAM_CODEC.encode(buffer, value.player.uuid)
                    buffer.writeOptional(Optional.ofNullable(value.hitResult?.location), Vec3.STREAM_CODEC)
                }
            }
    }
}
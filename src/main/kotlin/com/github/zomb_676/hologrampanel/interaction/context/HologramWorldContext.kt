package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.util.DistType
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.dynamic.Remember
import io.netty.buffer.Unpooled
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.RegistryAccess
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.neoforged.neoforge.common.extensions.ICommonPacketListener
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3fc

/**
 * describes the context with a bunch of objects where different widget target will require
 *
 * the trace will generate this and keep of it until invalid
 */
sealed interface HologramWorldContext : HologramContext {

    /**
     * the level will this object is created
     */
    @EfficientConst
    fun getLevel(): Level

    /**
     * the world position the hologram located in world
     *
     *  the return value should not be modified
     */
    fun hologramCenterPosition(): Vector3fc

    /**
     * for entity should consider interpolation
     */
    fun hologramCenterPosition(partialTick: Float): Vector3fc

    /**
     * identity object for check to avoid repeat create
     *
     * not using the return type to deduce something
     */
    @EfficientConst
    fun getIdentityObject(): Any

    /**
     * not null if created from ray trace
     */
    @EfficientConst
    fun getHitContext(): HitResult?

    /**
     * if the trace object is still alive or exist
     *
     * in this case, we think the tracing object is no longer valid
     *
     * this is considered as [com.github.zomb_676.hologrampanel.api.HologramTicket.isCritical] true
     */
    fun stillValid(): Boolean

    companion object {
        val STREAM_CODE: StreamCodec<FriendlyByteBuf, HologramWorldContext> =
            object : StreamCodec<FriendlyByteBuf, HologramWorldContext> {
                override fun decode(buffer: FriendlyByteBuf): HologramWorldContext = when (buffer.readShort()) {
                    1.toShort() -> BlockHologramContext.STREAM_CODEC.decode(buffer)
                    2.toShort() -> EntityHologramContext.STREAM_CODE.decode(buffer)
                    else -> throw RuntimeException()
                }

                override fun encode(buffer: FriendlyByteBuf, value: HologramWorldContext) = when (value) {
                    is BlockHologramContext -> {
                        buffer.writeShort(1)
                        BlockHologramContext.STREAM_CODEC.encode(buffer, value)
                    }

                    is EntityHologramContext -> {
                        buffer.writeShort(2)
                        EntityHologramContext.STREAM_CODE.encode(buffer, value)
                    }
                }
            }
    }
}
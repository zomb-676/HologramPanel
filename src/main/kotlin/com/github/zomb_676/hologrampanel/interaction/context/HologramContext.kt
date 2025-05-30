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
import net.neoforged.neoforge.common.extensions.ICommonPacketListener
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3fc

/**
 * describes the context with a bunch of objects where different widget target will require
 *
 * the trace will generate this and keep of it until invalid
 */
sealed interface HologramContext {

    /**
     * check if server installed this mod while current sever is not null
     *
     * set by [com.github.zomb_676.hologrampanel.payload.ServerHandShakePayload]
     */
    @EfficientConst.ConstDuringValidLifeCycle
    fun serverInstalled(): Boolean = HologramPanel.serverInstalled

    /**
     * the level will this object is created
     */
    @EfficientConst
    fun getLevel(): Level

    /**
     * the player who trig the context
     *
     *
     * [net.minecraft.client.player.LocalPlayer] at client
     *
     *
     * [net.minecraft.server.level.ServerPlayer] at server
     */
    @EfficientConst
    fun getPlayer(): Player

    /**
     * help to check on which logic side
     */
    @EfficientConst
    fun getLogicSide(): DistType

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
     */
    @EfficientConst
    fun getIdentityObject(): Any

    /**
     * @see Remember
     * @return notice that the [HologramContext] in generic must be the context it supports
     */
    @EfficientConst
    fun getRememberData(): Remember<out HologramContext>

    /**
     * @see Remember
     * @return notice that the [HologramContext] in generic must be the context it supports
     */
    @EfficientConst
    fun <T : HologramContext> getRememberDataUnsafe(): Remember<T> = getRememberData().unsafeCast()

    /**
     * if the trace object is still alive or exist
     *
     * in this case, we think the tracing object is no longer valid
     *
     * this is considered as [com.github.zomb_676.hologrampanel.api.HologramTicket.isCritical] true
     */
    fun stillValid(): Boolean

    @ApiStatus.NonExtendable
    fun getConnection(): ICommonPacketListener = if (this.getLogicSide().isClientSide) {
        (this.getPlayer() as LocalPlayer).connection
    } else {
        (this.getPlayer() as ServerPlayer).connection
    }

    /**
     * helper function to get a [RegistryAccess] or [net.minecraft.core.HolderLookup.Provider]
     */
    @ApiStatus.NonExtendable
    fun getRegistryAccess(): RegistryAccess = this.getLevel().registryAccess()

    /**
     * helper function to create a [RegistryFriendlyByteBuf]
     */
    @ApiStatus.NonExtendable
    fun createRegistryFriendlyByteBuf(): RegistryFriendlyByteBuf {
        return RegistryFriendlyByteBuf(Unpooled.buffer(), this.getRegistryAccess(), getConnection().connectionType)
    }

    /**
     * helper function to create a [RegistryFriendlyByteBuf] from a [java.nio.ByteBuffer]
     */
    @ApiStatus.NonExtendable
    fun warpRegistryFriendlyByteBuf(data: ByteArray): RegistryFriendlyByteBuf {
        return RegistryFriendlyByteBuf(
            Unpooled.wrappedBuffer(data), getRegistryAccess(), getConnection().connectionType
        )
    }

    companion object {
        val STREAM_CODE: StreamCodec<FriendlyByteBuf, HologramContext> =
            object : StreamCodec<FriendlyByteBuf, HologramContext> {
                override fun decode(buffer: FriendlyByteBuf): HologramContext = when (buffer.readShort()) {
                    1.toShort() -> BlockHologramContext.STREAM_CODEC.decode(buffer)
                    2.toShort() -> EntityHologramContext.STREAM_CODE.decode(buffer)
                    else -> throw RuntimeException()
                }

                override fun encode(buffer: FriendlyByteBuf, value: HologramContext) = when (value) {
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
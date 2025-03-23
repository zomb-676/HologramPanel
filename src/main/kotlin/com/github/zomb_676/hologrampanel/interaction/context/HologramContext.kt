package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.util.DistType
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.dynamic.Remember
import io.netty.buffer.Unpooled
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
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
    fun hologramCenterPosition(partialTick : Float): Vector3fc

    /**
     * identity object for check to avoid repeat create
     */
    @EfficientConst
    fun getIdentityObject(): Any

    /**
     * not null if created from ray trace
     */
    @EfficientConst
    fun getHitContext(): HitResult?

    @ApiStatus.Internal
    fun setServerUpdateDat(tag: CompoundTag)


    /**
     * @see Remember
     * @return notice that the [HologramContext] in generic must be the context it supports
     */
    @EfficientConst
    fun getRememberData() : Remember<out HologramContext>

    /**
     * @see Remember
     * @return notice that the [HologramContext] in generic must be the context it supports
     */
    @EfficientConst
    fun <T : HologramContext> getRememberDataUnsafe() : Remember<T> = getRememberData().unsafeCast()

    /**
     * if the trace object is still alive or exist
     *
     * in this case, we think the tracing object is no longer valid
     */
    fun stillValid() : Boolean

    @ApiStatus.NonExtendable
    fun getConnection() : ICommonPacketListener = if (this.getLogicSide().isClientSide) {
        (this.getPlayer() as LocalPlayer).connection
    } else {
        (this.getPlayer() as ServerPlayer).connection
    }

    @ApiStatus.NonExtendable
    fun getRegistryAccess(): RegistryAccess = this.getLevel().registryAccess()

    @ApiStatus.NonExtendable
    fun createRegistryFriendlyByteBuf(): RegistryFriendlyByteBuf {
        return RegistryFriendlyByteBuf(Unpooled.buffer(), this.getRegistryAccess(), getConnection().connectionType)
    }

    @ApiStatus.NonExtendable
    fun warpRegistryFriendlyByteBuf(data : ByteArray): RegistryFriendlyByteBuf {
        return RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), getRegistryAccess(), getConnection().connectionType)
    }
}
package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.api.GenericThis
import com.github.zomb_676.hologrampanel.util.DistType
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.dynamic.Remember
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3fc

/**
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
    fun attachedServerData() : CompoundTag?

    @ApiStatus.Internal
    fun setServerUpdateDat(tag: CompoundTag)

    /**
     * get data query from server here
     * why [CompoundTag]?, data can support check existence and arbitrary sort
     *
     * @return must be [GenericThis]
     */
    @EfficientConst
    fun getRememberData() : Remember<out HologramContext>

    @EfficientConst
    fun <T : HologramContext> getRememberDataUnsafe() : Remember<T> = getRememberData().unsafeCast()
}
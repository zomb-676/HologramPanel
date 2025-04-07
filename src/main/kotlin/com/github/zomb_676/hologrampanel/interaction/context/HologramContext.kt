package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.util.DistType
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.dynamic.Remember
import io.netty.buffer.Unpooled
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.extensions.ICommonPacketListener
import org.jetbrains.annotations.ApiStatus

sealed interface HologramContext {
    /**
     * check if the server installed this mod while current sever is not null
     *
     * set by [com.github.zomb_676.hologrampanel.payload.ServerHandShakePayload]
     */
    @EfficientConst.ConstDuringValidLifeCycle
    fun serverInstalled(): Boolean = HologramPanel.serverInstalled

    /**
     * the player who trig the context
     *
     * [net.minecraft.client.player.LocalPlayer] at client
     *
     * [net.minecraft.server.level.ServerPlayer] at server
     */
    @EfficientConst
    fun getPlayer(): Player

    /**
     * help to check on which logic side
     */
    @EfficientConst
    fun getLogicSide(): DistType = DistType.from(getPlayer().level())

    /**
     * @see Remember
     * @return notice that the [HologramWorldContext] in generic must be the context it supports
     */
    @EfficientConst
    fun getRememberData(): Remember<out HologramContext>

    /**
     * @see Remember
     * @return notice that the [HologramWorldContext] in generic must be the context it supports
     */
    @EfficientConst
    fun <T : HologramContext> getRememberDataUnsafe(): Remember<T> = getRememberData().unsafeCast()

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
    fun getRegistryAccess(): RegistryAccess = this.getPlayer().level().registryAccess()

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
}
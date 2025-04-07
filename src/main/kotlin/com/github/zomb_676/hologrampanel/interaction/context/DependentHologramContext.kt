package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.widget.dynamic.Remember
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.entity.player.Player

/**
 * this context is used when [com.github.zomb_676.hologrampanel.widget.HologramWidget] is
 * trig not by
 */
//class DependentHologramContext<T : Any>(private val player: Player, private val data: T, val codec: StreamCodec<RegistryFriendlyByteBuf, T>) :
//    HologramContext {
//
//    val remember = Remember.create(this)
//
//    override fun getPlayer(): Player = player
//
//    override fun getRememberData(): Remember<DependentHologramContext<T>> = remember
//}
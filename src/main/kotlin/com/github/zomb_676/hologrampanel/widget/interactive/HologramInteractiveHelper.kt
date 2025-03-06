package com.github.zomb_676.hologrampanel.widget.interactive

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.HologramCreatePayload
import com.github.zomb_676.hologrampanel.sync.DataSynchronizer
import com.github.zomb_676.hologrampanel.sync.SynchronizerManager
import com.github.zomb_676.hologrampanel.util.unsafeCast
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.common.util.FriendlyByteBufUtil
import java.util.*
import java.util.function.Consumer

object HologramInteractiveHelper {

    private val factories: MutableMap<HologramInteractiveTarget.Provider<*>, Ins<*>> = mutableMapOf()

    private data class Ins<V : HologramInteractiveTarget>(
        val target: HologramInteractiveTarget.Provider<V>,
        val factory: (V) -> HologramInteractiveWidget<V>
    )

    fun <V : HologramInteractiveTarget> register(
        target: HologramInteractiveTarget.Provider<V>,
        factory: (V) -> HologramInteractiveWidget<V>
    ) {
        val ins = Ins(target, factory)
        factories[target] = ins
    }

    fun <T : HologramInteractiveTarget> create(target: T): HologramInteractiveWidget<T> {
        val ins = factories[target.provider] ?: throw RuntimeException()
        return ins.unsafeCast<Ins<T>>().factory.invoke(target)
    }

    fun <T : HologramInteractiveTarget> addClientWidget(widget: HologramInteractiveWidget<T>, context: HologramContext) {
        val synchronizer = widget.target.synchronizer
        SynchronizerManager.Client.syncers.put(synchronizer.uuid, synchronizer)
        HologramManager.tryAddWidget(widget, context)
    }

    fun <T : HologramInteractiveTarget> addServer(player: ServerPlayer, target: T) {
        val synchronizer = target.synchronizer
        SynchronizerManager.Server.addForPlayer(player, synchronizer)
    }

    fun <T : HologramInteractiveTarget> openOnServer(
        player: ServerPlayer,
        creator: HologramInteractiveTarget.Provider<T>,
        additionDataWriter: Consumer<RegistryFriendlyByteBuf> = Consumer {}
    ) {
        val uuid = UUID.randomUUID()
        val additionData = FriendlyByteBufUtil.writeCustomData(additionDataWriter, player.registryAccess())
        player.connection.send(HologramCreatePayload(uuid, creator, additionData))
        val buffer = RegistryFriendlyByteBuf(
            Unpooled.wrappedBuffer(additionData),
            player.registryAccess(),
            player.connection.connectionType
        )
        val interactiveTarget = creator.create(
            player,
            DistType.SERVER,
            DataSynchronizer(uuid, DistType.SERVER, player.connection, player.registryAccess()),
            buffer
        )
        addServer(player, interactiveTarget)
    }
}
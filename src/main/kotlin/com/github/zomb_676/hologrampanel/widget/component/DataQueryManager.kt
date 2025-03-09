package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.ComponentRequestDataPayload
import com.github.zomb_676.hologrampanel.payload.ComponentResponseDataPayload
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import java.util.*

object DataQueryManager {
    object Client {
        private val syncs: MutableMap<UUID, HologramComponentWidget<*>> = mutableMapOf()
        private val maps: MutableMap<HologramComponentWidget<*>, HologramContext> = mutableMapOf()

        fun <T : HologramContext> query(
            widget: HologramComponentWidget<out T>,
            additionDataTag: CompoundTag,
            providers: MutableList<ServerDataProvider<T>>,
            context: T
        ) {
            val uuid = UUID.randomUUID()
            syncs[uuid] = widget
            maps[widget] = context
            val payload = ComponentRequestDataPayload(uuid, additionDataTag, providers, context)
            Minecraft.getInstance().player!!.connection.send(payload)
        }

        fun receiveData(uuid: UUID, tag: CompoundTag) {
            val widget = syncs[uuid]
            if (widget == null) {
                //todo
                return
            }
            maps[widget]!!.let { context ->
                context.setServerUpdateDat(tag)
                context.getRememberData().onReceiveData(tag)
            }
        }

        fun closeForWidget() {

        }

        fun closeAll() {
            this.syncs.clear()
            this.maps.clear()
        }
    }

    object Server {
        private val syncs: MutableMap<ServerPlayer, MutableList<ComponentRequestDataPayload<*>>> = mutableMapOf()
        private var lastSyncTick: Int = 5

        fun <T : HologramContext> create(player: ServerPlayer, payload: ComponentRequestDataPayload<T>) {
            syncs.computeIfAbsent(player) { mutableListOf() }.add(payload)
        }

        fun tick() {
            if (--lastSyncTick != 0) return
            lastSyncTick = 5

            syncs.forEach { (player, payloads) ->
                for (payload in payloads) {
                    val tag = CompoundTag()
                    append(payload, tag)
                    val payload = ComponentResponseDataPayload(payload.uuid, tag)
                    player.connection.send(payload)
                }
            }
        }

        private fun <T : HologramContext> append(payload: ComponentRequestDataPayload<T>, tag: CompoundTag) {
            payload.providers.forEach { provider ->
                provider.appendServerData(payload.additionDataTag, tag, payload.context)
            }
        }

        fun clearForPlayer(player: ServerPlayer) {
            syncs.remove(player)
        }
    }
}
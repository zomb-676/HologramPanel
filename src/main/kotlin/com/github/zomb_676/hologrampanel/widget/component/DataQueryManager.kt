package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.ComponentRequestDataPayload
import com.github.zomb_676.hologrampanel.payload.ComponentResponseDataPayload
import com.github.zomb_676.hologrampanel.payload.SyncClosePayload
import com.github.zomb_676.hologrampanel.util.AutoTicker
import com.github.zomb_676.hologrampanel.util.profilerStack
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import java.util.*

object DataQueryManager {
    object Client {
        private val syncs: MutableMap<UUID, DynamicBuildWidget<*>> = mutableMapOf()
        private val maps: MutableMap<DynamicBuildWidget<*>, HologramContext> = mutableMapOf()

        fun <T : HologramContext> query(
            widget: DynamicBuildWidget<T>,
            additionDataTag: CompoundTag,
            providers: List<ServerDataProvider<T,*>>,
            context: T
        ) {
            val uuid = context.getRememberData().uuid
            syncs[uuid] = widget
            maps[widget] = context
            val payload = ComponentRequestDataPayload(uuid, additionDataTag, providers, context)
            Minecraft.getInstance().player!!.connection.send(payload)
        }

        fun receiveData(uuid: UUID, tag: CompoundTag) {
            val widget = syncs[uuid]
            if (widget == null) {
                SyncClosePayload(uuid).sendToServer()
                return
            }
            val context = maps[widget]!!

            context.setServerUpdateDat(tag)
            context.getRememberData().onReceiveData(tag)
            if (context.getRememberData().needUpdate()) {
                profilerStack("rebuild_hologram_component") {
                    val state = HologramManager.queryHologramState(widget) ?: return@profilerStack
                    widget.updateComponent(state.displayType)
                }
            }
        }

        fun closeForWidget(widget: DynamicBuildWidget<*>) {
            maps.remove(widget)
            syncs.remove(widget.target.getRememberData().uuid)
        }

        fun closeForWidget(uuid : UUID) {
            val widget = syncs.remove(uuid) ?: return
            maps.remove(widget)
        }

        fun closeAll() {
            this.syncs.clear()
            this.maps.clear()
        }
    }

    object Server {
        private val syncs: MutableMap<ServerPlayer, MutableMap<UUID, ComponentRequestDataPayload<*>>> = mutableMapOf()
        private val tick = AutoTicker.by(Config.Server.updateInternal::get)

        fun <T : HologramContext> create(player: ServerPlayer, payload: ComponentRequestDataPayload<T>) {
            syncs.computeIfAbsent(player) { mutableMapOf() }[payload.uuid] = (payload)
        }

        fun tick() {
            tick.tryConsume {
                syncs.forEach { (player, payloads) ->
                    for (payload in payloads.values) {
                        val tag = CompoundTag()
                        val changed = append(payload, tag)
                        if (changed) {
                            val payload = ComponentResponseDataPayload(payload.uuid, tag)
                            player.connection.send(payload)
                        }
                    }
                }
            }
        }

        private fun <T : HologramContext> append(payload: ComponentRequestDataPayload<T>, tag: CompoundTag): Boolean {
            var changed = false
            payload.providers.forEach { provider ->
                val addTag = CompoundTag()
                tag.put(provider.location().toString(), addTag)
                changed = changed or provider.appendServerData(payload.additionDataTag, addTag, payload.context)
            }
            return changed
        }

        fun clearForPlayer(player: ServerPlayer) {
            syncs.remove(player)
        }

        fun closeWidget(player: ServerPlayer, uuid: UUID) {
            syncs[player]?.remove(uuid)
        }
    }
}
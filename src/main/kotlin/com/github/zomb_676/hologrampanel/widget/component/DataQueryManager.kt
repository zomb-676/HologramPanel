package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.DebugHelper
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.ComponentRequestDataPayload
import com.github.zomb_676.hologrampanel.payload.ComponentResponseDataPayload
import com.github.zomb_676.hologrampanel.payload.SyncClosePayload
import com.github.zomb_676.hologrampanel.util.AutoTicker
import com.github.zomb_676.hologrampanel.util.profilerStack
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableMap
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import org.joml.Vector3fc
import java.util.*
import kotlin.math.acos

/**
 * manager object for all [com.github.zomb_676.hologrampanel.widget.dynamic.Remember]
 */
object DataQueryManager {
    object Client {
        private val syncs: BiMap<UUID, DynamicBuildWidget<*>> = HashBiMap.create()
        private val maps: BiMap<DynamicBuildWidget<*>, HologramContext> = HashBiMap.create()

        fun syncCount() = syncs.size

        fun <T : HologramContext> query(
            widget: DynamicBuildWidget<T>,
            additionDataTag: CompoundTag,
            providers: List<ServerDataProvider<T, *>>,
            context: T
        ) {
            val uuid = context.getRememberData().uuid
            syncs[uuid] = widget
            maps[widget] = context
            val payload = ComponentRequestDataPayload(uuid, additionDataTag, providers, context)
            Minecraft.getInstance().player!!.connection.send(payload)
        }

        fun queryContextUUID(context: HologramContext): UUID? {
            val widget = maps.inverse()[context] ?: return null
            return syncs.inverse()[widget]
        }

        fun receiveData(uuid: UUID, data: ImmutableMap<ComponentProvider<*, *>, CompoundTag>, sizeInBytes: Int) {
            val widget = syncs[uuid]
            if (widget == null) {
                SyncClosePayload(uuid).sendToServer()
                return
            }
            val context = maps[widget]!!

            context.getRememberData().onReceiveData(data)
            if (context.getRememberData().needUpdate()) {
                profilerStack("rebuild_hologram_component") {
                    val state = HologramManager.queryHologramState(widget) ?: return@profilerStack
                    widget.updateComponent(state.displayType, false)
                }
            }
            DebugHelper.Client.onDataReceived(widget, sizeInBytes)
        }

        fun closeForWidget(widget: DynamicBuildWidget<*>) {
            maps.remove(widget)
            syncs.remove(widget.target.getRememberData().uuid)
        }

        fun closeForWidget(uuid: UUID) {
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

        fun syncCount() = syncs.values.sumOf { it.size }
        fun syncCountForPlayer(serverPlayer: ServerPlayer) =
            syncs[serverPlayer]?.size ?: 0

        fun <T : HologramContext> create(player: ServerPlayer, payload: ComponentRequestDataPayload<T>) {
            syncs.computeIfAbsent(player) { mutableMapOf() }[payload.uuid] = (payload)
        }

        fun manualSync(syncUUID: UUID, player: ServerPlayer) {
            val payload = syncs[player]?.get(syncUUID) ?: return
            val builder: ImmutableMap.Builder<ComponentProvider<*, *>, CompoundTag> = ImmutableMap.builder()
            val changed = append(payload, builder)
            if (!changed) return
            val data = builder.build()
            if (data.isNotEmpty()) {
                val payload = ComponentResponseDataPayload.of(payload.uuid, data)
                player.connection.send(payload)
            }
        }

        fun tick() {
            tick.tryConsume {
                val syncRadius = Config.Server.syncRadius.get()
                syncs.forEach { (player, payloads) ->
                    val playerPos = player.blockPosition()
                    for (payload in payloads.values) {
                        val pos = when (val context = payload.context) {
                            is BlockHologramContext -> context.pos
                            is EntityHologramContext -> context.getEntity().blockPosition()
                        }
                        //skip not loaded
                        if (!Config.Server.updateAtUnloaded.get()) {
                            if (!payload.context.getLevel().isLoaded(pos)) continue
                        }
                        //skip far hologram
                        if (playerPos.distSqr(pos) > syncRadius * syncRadius) continue
                        //skip not in visual angles
                        val degree = calculateDegree(player, payload.context.hologramCenterPosition())
                        if (degree > 80) continue

                        val builder: ImmutableMap.Builder<ComponentProvider<*, *>, CompoundTag> = ImmutableMap.builder()
                        val changed = append(payload, builder)
                        if (!changed) continue
                        val data = builder.build()
                        if (data.isNotEmpty()) {
                            val payload = ComponentResponseDataPayload.of(payload.uuid, data)
                            player.connection.send(payload)
                        }
                    }
                }
            }
        }

        private fun calculateDegree(serverPlayer: ServerPlayer, sourcePosition: Vector3fc): Double {
            val viewVector = serverPlayer.getViewVector(1f).normalize()
            val cameraPosition = serverPlayer.eyePosition
            val sourceVector = Vec3(
                (sourcePosition.x() - cameraPosition.x),
                (sourcePosition.y() - cameraPosition.y),
                (sourcePosition.z() - cameraPosition.z)
            ).normalize()

            val dot = viewVector.dot(sourceVector)
            val angleInRadius = acos(dot)
            return Math.toDegrees(angleInRadius.toDouble())
        }

        private fun <T : HologramContext> append(payload: ComponentRequestDataPayload<T>, tag: ImmutableMap.Builder<ComponentProvider<*, *>, CompoundTag>): Boolean {
            var changed = false
            payload.providers.forEach { provider ->
                val addTag = CompoundTag()
                tag.put(provider, addTag)
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
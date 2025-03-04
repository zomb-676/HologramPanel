package com.github.zomb_676.hologrampanel.sync

import net.minecraft.server.level.ServerPlayer
import java.util.*

object SynchronizerManager {
    object Server {
        val syncers: MutableMap<UUID, DataSynchronizer> = mutableMapOf()
        val playerMap: MutableMap<ServerPlayer, MutableList<DataSynchronizer>> = mutableMapOf()

        fun clearForPlayer(serverPlayer: ServerPlayer) {
            val playerSyncers = playerMap[serverPlayer] ?: return
            playerSyncers.forEach { syncers.remove(it.uuid) }
            playerMap.remove(serverPlayer)
        }

        fun addForPlayer(player: ServerPlayer, syncer: DataSynchronizer) {
            syncers[syncer.uuid] = syncer
            playerMap.computeIfAbsent(player) { mutableListOf() }.add(syncer)
        }
    }

    object Client {
        val syncers: MutableMap<UUID, DataSynchronizer> = mutableMapOf()
    }
}
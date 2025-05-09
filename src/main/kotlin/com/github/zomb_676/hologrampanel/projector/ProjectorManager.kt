package com.github.zomb_676.hologrampanel.projector

import com.github.zomb_676.hologrampanel.interaction.HologramRenderState

object ProjectorManager {
    private val projectorListeners: MutableList<ProjectorBlockEntity> = mutableListOf()

    fun add(entity: ProjectorBlockEntity) {
        projectorListeners.add(entity)
    }

    fun remove(entity: ProjectorBlockEntity) {
        projectorListeners.remove(entity)
    }

    fun checkProjectorSetting(state: HologramRenderState) {
        val blockEntity = projectorListeners.firstOrNull { it.checkTransform(state) } ?: return
        blockEntity.setStateLocate(state)
    }

    fun getListeners(): List<ProjectorBlockEntity> = this.projectorListeners

    fun clear() {
        this.projectorListeners.clear()
    }
}
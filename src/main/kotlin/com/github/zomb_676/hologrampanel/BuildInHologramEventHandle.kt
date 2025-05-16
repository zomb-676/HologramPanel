package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.event.HologramEvent
import com.github.zomb_676.hologrampanel.projector.ProjectorManager
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.NeoForge

object BuildInHologramEventHandle {
    fun initEvents(dist: Dist, modBus: IEventBus) {
        val forgeBus = NeoForge.EVENT_BUS
        forgeBus.addListener(::onWidgetAdded)
        forgeBus.addListener(::onWidgetRemoved)
    }

    fun onWidgetAdded(event: HologramEvent.AddPost<*>) {
        ProjectorManager.checkProjectorSetting(event.getHologramState())
    }

    fun onWidgetRemoved(event: HologramEvent.RemovePre<*>) {

    }
}
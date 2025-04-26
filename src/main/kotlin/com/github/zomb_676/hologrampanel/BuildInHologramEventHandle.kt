package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.event.HologramEvent
import com.github.zomb_676.hologrampanel.projector.ProjectorManager
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.IEventBus

object BuildInHologramEventHandle {
    fun initEvents(dist: Dist, modBus: IEventBus) {
        val forgeBus = MinecraftForge.EVENT_BUS
        forgeBus.addListener(::onWidgetAdded)
        forgeBus.addListener(::onWidgetRemoved)
    }

    fun onWidgetAdded(event: HologramEvent.Add<*>) {
        ProjectorManager.checkProjectorSetting(event.getHologramState())
    }

    fun onWidgetRemoved(event: HologramEvent.Remove<*>) {

    }
}
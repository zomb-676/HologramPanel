package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.util.AxisMode
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector
import com.github.zomb_676.hologrampanel.util.selector.CycleSelectorBuilder
import com.github.zomb_676.hologrampanel.widget.element.ComponentRenderElement
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object PanelOperatorManager {
    var selectedTarget: HologramRenderState? = null
        private set
        get() {
            val result = field
            if (result == null || result.removed) {
                field = null
                return null
            } else {
                return result
            }
        }

    var axisMode = AxisMode.LOCAL
        private set

    fun createInstance(): CycleSelector? {
        return CycleSelectorBuilder {
            if (selectedTarget == null) {
                add(ComponentRenderElement("Set Modify Target").setScale(0.8)) {
                    val hologram = HologramManager.getInteractHologram()
                    val message = if (hologram != null) {
                        selectedTarget = hologram
                        Component.literal("success set")
                    } else Component.literal("failed to set")
                    Minecraft.getInstance().gui.chat.addMessage(message)
                }
                repeat(3) {
                    add(ComponentRenderElement("placeholder")) {

                    }
                }
            } else {
                add(ComponentRenderElement("Clear Modify Target").setScale(0.8)) {
                    selectedTarget = null
                }
                addGroup(ComponentRenderElement("Set Axis Mode").setScale(0.8)) {
                    add(ComponentRenderElement("world axis").setScale(0.8)) {
                        axisMode = AxisMode.WORLD
                    }
                    add(ComponentRenderElement("local axis").setScale(0.8)) {
                        axisMode = AxisMode.LOCAL
                    }
                    add(ComponentRenderElement("player axis").setScale(0.8)) {
                        axisMode = AxisMode.PLAYER
                    }
                }
            }
        }
    }
}
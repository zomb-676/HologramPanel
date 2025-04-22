package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.util.AxisMode
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector
import com.github.zomb_676.hologrampanel.util.selector.CycleSelectorBuilder
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.github.zomb_676.hologrampanel.widget.element.ComponentRenderElement
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import org.joml.Vector3f

object PanelOperatorManager {
    var modifyTarget: HologramRenderState? = null
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
            if (modifyTarget == null) {
                add(ComponentRenderElement("Set Modify Target").setScale(0.8)) {
                    val hologram = HologramManager.getInteractHologram() ?: return@add
                    val message = if (hologram.locate is LocateType.World.FacingVector) {
                        modifyTarget = hologram
                        Component.literal("success set")
                    } else Component.literal("failed to set")
                    Minecraft.getInstance().gui.chat.addMessage(message)
                }
                repeat(3) {
                    add(ComponentRenderElement("$it")) {

                    }
                }
            } else {
                add(ComponentRenderElement("Clear Modify Target").setScale(0.8)) {
                    modifyTarget = null
                }
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
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
import org.joml.Vector2f
import kotlin.math.sqrt

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
            val createTimeHologram = HologramManager.getInteractHologram()
            if (selectedTarget == null) {
                add(ComponentRenderElement("Set Selected").setScale(0.8)) {
                    val hologram = createTimeHologram?.takeIf { !it.removed } ?: HologramManager.getInteractHologram()
                    val message = if (hologram != null) {
                        selectedTarget = hologram
                        Component.literal("success set")
                    } else Component.literal("failed to set")
                    Minecraft.getInstance().gui.chat.addMessage(message)
                }
            } else {
                add(ComponentRenderElement("Clear Selected").setScale(0.8)) {
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
            addGroup(ComponentRenderElement("LocateType").setScale(0.8)) {
                add(ComponentRenderElement("pin screen").setScale(0.8)) {
                    val target = findTarget(createTimeHologram) ?: return@add
                    if (target.locate !is LocateType.Screen) {
                        val old = target.locate
                        target.locate = LocateType.Screen(Vector2f())
                        HologramManager.notifyHologramLocateTypeChange(target, old)
                    }
                }
                add(ComponentRenderElement("pin world").setScale(0.8)) {
                    val target = findTarget(createTimeHologram) ?: return@add
                    val camera = Minecraft.getInstance().gameRenderer.mainCamera
                    when (val locate = target.locate) {
                        is LocateType.World.FacingVector -> locate.byCamera(camera)
                        else -> {
                            target.locate = LocateType.World.FacingVector().byCamera(camera).apply {
                                camera.lookVector.mul(-sqrt(3f) / 2f, offset)
                            }
                            HologramManager.notifyHologramLocateTypeChange(target, locate)
                        }
                    }
                }
                add(ComponentRenderElement("face player").setScale(0.8)) {
                    val target = findTarget(createTimeHologram) ?: return@add
                    if (target.locate !is LocateType.World.FacingPlayer) {
                        val old = target.locate
                        target.locate = LocateType.World.FacingPlayer
                        HologramManager.notifyHologramLocateTypeChange(target, old)
                    }
                }
            }
        }
    }

    private fun findTarget(createTime: HologramRenderState?): HologramRenderState? =
        this.selectedTarget ?: createTime?.takeIf { !it.removed } ?: HologramManager.getInteractHologram()
}
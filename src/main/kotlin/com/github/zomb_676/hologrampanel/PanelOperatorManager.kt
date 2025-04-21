package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector
import com.github.zomb_676.hologrampanel.util.selector.CycleSelectorBuilder
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.github.zomb_676.hologrampanel.widget.element.ComponentRenderElement
import com.github.zomb_676.hologrampanel.widget.element.ItemStackElement
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

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

    private enum class Mode {}

    fun createInstance(): CycleSelector? {
        return CycleSelectorBuilder {
            if (modifyTarget == null) {
                add(ComponentRenderElement("Set Modify Target").setScale(0.5)) {
                    val hologram = HologramManager.getInteractHologram() ?: return@add
                    val message = if (hologram.locate is LocateType.World.FacingVector) {
                        modifyTarget = hologram
                        Component.literal("success set")
                    } else Component.literal("failed to set")
                    Minecraft.getInstance().gui.chat.addMessage(message)
                }
            } else {
                add(ComponentRenderElement("Clear Modify Target").setScale(0.5)) {
                    modifyTarget = null
                }
            }
            repeat(3) {
                add(ComponentRenderElement("$it")) {

                }
            }
            addGroup(ItemStackElement(false, ItemStack(Items.DIRT))) {
                repeat(5) { index ->
                    add(ComponentRenderElement("inner$index")) {
                        Minecraft.getInstance().gui.chat.addMessage(Component.literal("inner$index"))
                    }
                }
            }
        }
    }
}
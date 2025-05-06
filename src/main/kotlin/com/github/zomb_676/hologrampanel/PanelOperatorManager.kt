package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.RayTraceHelper
import com.github.zomb_676.hologrampanel.util.AxisMode
import com.github.zomb_676.hologrampanel.util.modifyAndSave
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector
import com.github.zomb_676.hologrampanel.util.selector.CycleSelectorBuilder
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.github.zomb_676.hologrampanel.widget.element.ComponentRenderElement
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
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
    var modifyLocation : Boolean = false
        private set

    fun createInstance(): CycleSelector? {
        return CycleSelectorBuilder {
            val createTimeHologram = HologramManager.getInteractHologram()
            add {
                renderElement {
                    if (selectedTarget == null) {
                        ComponentRenderElement("Set Selected").setScale(0.8)
                    } else {
                        ComponentRenderElement("Clear Selected").setScale(0.8)
                    }
                }
                onClick {
                    if (selectedTarget == null) {
                        val hologram = createTimeHologram?.takeIf { !it.removed } ?: HologramManager.getInteractHologram()
                        val message = if (hologram != null) {
                            selectedTarget = hologram
                            Component.literal("success set")
                        } else Component.literal("failed to set")
                        addMessage(message)
                    } else {
                        selectedTarget = null
                        addMessage(Component.literal("clear selected"))
                    }
                }
            }
            addGroup(ComponentRenderElement("Set Axis Mode").setScale(0.8)) {
                add(ComponentRenderElement("world axis").setScale(0.8)) {
                    axisMode = AxisMode.WORLD
                    addMessage("switch Axis Mode to World Axis")
                }
                add(ComponentRenderElement("local axis").setScale(0.8)) {
                    axisMode = AxisMode.LOCAL
                    addMessage("switch Axis Mode to Local Axis")
                }
                add(ComponentRenderElement("player axis").setScale(0.8)) {
                    axisMode = AxisMode.PLAYER
                    addMessage("switch Axis Mode to Player Axis")
                }
                add(ComponentRenderElement("position").setScale(0.8)) {
                    modifyLocation = true
                }
                add(ComponentRenderElement("rotation").setScale(0.8)) {
                    modifyLocation = false
                }
            }
            addGroup(ComponentRenderElement("Modify Selected").setScale(0.8)) {
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
            addGroup(ComponentRenderElement("hide").setScale(0.8)) {
                add {
                    notClickOnClose()
                    var entity: EntityType<*>? = null
                    tick {
                        entity = findEntity()?.entity?.type
                    }
                    renderElement {
                        val entity = entity
                        if (entity != null) {
                            if (checkEntityContains(entity)) {
                                ComponentRenderElement("not hide entity ${entity.location()}").setScale(0.8)
                            } else {
                                ComponentRenderElement("hide entity ${entity.location()}").setScale(0.8)
                            }
                        } else {
                            ComponentRenderElement("see entity to hide").setScale(0.8)
                        }
                    }
                    onClick {
                        val entry = entity?.location()?.toString()
                        if (entry == null) {
                            addMessage("see entity to hide")
                        } else if (!Config.Client.hideEntityTypes.get().contains(entry)) {
                            Config.Client.hideEntityTypes.modifyAndSave {
                                it += entry
                            }
                            addMessage("hide entity $entry")
                            HologramManager.checkAllHologramByPrevent()
                        } else {
                            Config.Client.hideEntityTypes.modifyAndSave {
                                it -= entry
                            }
                            addMessage("not hide entity $entry")
                        }
                    }
                }
                add {
                    notClickOnClose()
                    var block: Block? = null
                    tick {
                        block = findBlock()?.run { Minecraft.getInstance().level?.getBlockState(blockPos)?.block }
                    }
                    renderElement {
                        val block = block
                        if (block != null) {
                            if (checkBlockContains(block)) {
                                ComponentRenderElement("not hide block ${block.location()}").setScale(0.8)
                            } else {
                                ComponentRenderElement("hide block ${block.location()}").setScale(0.8)
                            }
                        } else {
                            ComponentRenderElement("see block to hide").setScale(0.8)
                        }
                    }
                    onClick {
                        val entry = block?.location()?.toString()
                        if (entry == null) {
                            addMessage("see block to hide")
                        } else if (!Config.Client.hideBlocks.get().contains(entry)) {
                            Config.Client.hideBlocks.modifyAndSave {
                                it += entry
                            }
                            addMessage("hide Block $entry")
                            HologramManager.checkAllHologramByPrevent()
                        } else {
                            Config.Client.hideBlocks.modifyAndSave {
                                it -= entry
                            }
                            addMessage("not hide Block $entry")
                        }
                    }
                }
            }
        }
    }

    private fun findTarget(createTime: HologramRenderState?): HologramRenderState? =
        this.selectedTarget ?: createTime?.takeIf { !it.removed } ?: HologramManager.getInteractHologram()

    private fun findBlock() =
        RayTraceHelper.rayTraceBlock(32.0, 1f)

    private fun findEntity() =
        RayTraceHelper.rayTraceEntity(32.0, 1f)

    private fun EntityType<*>.location() = BuiltInRegistries.ENTITY_TYPE.getKey(this)
    private fun Block.location() = BuiltInRegistries.BLOCK.getKey(this)

    private fun addMessage(message: Component) = Minecraft.getInstance().gui.chat.addMessage(message)

    private fun addMessage(message: String) = addMessage(Component.literal(message))

    private fun checkBlockContains(block: Block) =
        Config.Client.hideBlocks.get().contains(block.location().toString())

    private fun checkEntityContains(entity: EntityType<*>) =
        Config.Client.hideEntityTypes.get().contains(entity.location().toString())
}
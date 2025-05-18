package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.RayTraceHelper
import com.github.zomb_676.hologrampanel.projector.IHologramStorage
import com.github.zomb_676.hologrampanel.util.addClientMessage
import com.github.zomb_676.hologrampanel.util.modifyAndSave
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector
import com.github.zomb_676.hologrampanel.util.selector.CycleSelectorBuilder
import com.github.zomb_676.hologrampanel.widget.element.ComponentRenderElement
import com.github.zomb_676.hologrampanel.widget.locateType.*
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import org.joml.Vector2f
import kotlin.math.sqrt

object PanelOperatorManager {
    var selectedTarget: HologramRenderState? = null
        set(value) {
            if (value != null && value.removed) return
            field = value
        }
        get() {
            val result = field
            if (result == null || result.removed) {
                field = null
                return null
            } else {
                return result
            }
        }

    var transformOrientation = TransformOrientation.LOCAL
        private set
    var modifyLocation: AdjustType = AdjustType.LOCATION
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
                        addClientMessage(message)
                    } else {
                        selectedTarget = null
                        addClientMessage(Component.literal("clear selected"))
                    }
                }
            }
            addGroup {
                adjustGroup {
                    renderElement(ComponentRenderElement("TransformOrientation").setScale(0.8))
                    visible { selectedTarget.run { this != null && this.locate is LocateInWorld } }
                }

                add(ComponentRenderElement("world axis").setScale(0.8)) {
                    transformOrientation = TransformOrientation.WORLD
                    addClientMessage("switch Axis Mode to World Axis")
                }
                add {
                    renderElement { ComponentRenderElement("local axis").setScale(0.8) }
                    onClick {
                        transformOrientation = TransformOrientation.LOCAL
                        addClientMessage("switch Axis Mode to Local Axis")
                    }
                    visible { selectedTarget.run { this != null && this.locate !is LocateFacingPlayer } }
                }
                add(ComponentRenderElement("player axis").setScale(0.8)) {
                    transformOrientation = TransformOrientation.PLAYER
                    addClientMessage("switch Axis Mode to Player Axis")
                }
                add {
                    renderElement {
                        if (modifyLocation == AdjustType.LOCATION) {
                            ComponentRenderElement("location", 0xffffffff.toInt()).setScale(0.8)
                        } else {
                            ComponentRenderElement("location", 0xff000000.toInt()).setScale(0.8)
                        }
                    }
                    onClick {
                        modifyLocation = AdjustType.LOCATION
                    }
                }
                add {
                    renderElement {
                        if (modifyLocation == AdjustType.ROTATION) {
                            ComponentRenderElement("rotation", 0xffffffff.toInt()).setScale(0.8)
                        } else {
                            ComponentRenderElement("rotation", 0xff000000.toInt()).setScale(0.8)
                        }
                    }
                    onClick {
                        modifyLocation = AdjustType.ROTATION
                    }
                    visible {
                        selectedTarget.run { this != null && this.locate !is LocateFacingPlayer }
                    }
                }
            }
            addGroup {
                adjustGroup {
                    renderElement(ComponentRenderElement("Set LocateType").setScale(0.8))
                    visible { selectedTarget != null }
                }
                add(ComponentRenderElement("LocateOnScreen").setScale(0.8)) {
                    val target = findTarget(createTimeHologram) ?: return@add
                    if (target.locate !is LocateOnScreen) {
                        val old = target.locate
                        target.locate = LocateOnScreen(Vector2f())
                        HologramManager.notifyHologramLocateTypeChange(target, old)
                    }
                }
                add(ComponentRenderElement("LocateFreelyInWorld").setScale(0.8)) {
                    val target = findTarget(createTimeHologram) ?: return@add
                    val camera = Minecraft.getInstance().gameRenderer.mainCamera
                    when (val locate = target.locate) {
                        is LocateFreelyInWorld -> locate.byCamera(camera)
                        else -> {
                            target.locate = LocateFreelyInWorld().byCamera(camera).apply {
                                camera.lookVector.mul(-sqrt(3f) / 2f, offset)
                            }
                            HologramManager.notifyHologramLocateTypeChange(target, locate)
                        }
                    }
                }
                add(ComponentRenderElement("LocateFacePlayer").setScale(0.8)) {
                    val target = findTarget(createTimeHologram) ?: return@add
                    if (target.locate !is LocateFacingPlayer) {
                        val old = target.locate
                        target.locate = LocateFacingPlayer()
                        HologramManager.notifyHologramLocateTypeChange(target, old)
                    }
                }
                run {
                    val block = findBlock() ?: return@run
                    val level = Minecraft.getInstance().level ?: return@run
                    val be = level.getBlockEntity(block.blockPos) ?: return@run
                    val storage = level.getCapability(IHologramStorage.CAPABILITY, block.blockPos) ?: return@run
                    add(ComponentRenderElement("bind to target").setScale(0.8)) {
                        val target = findTarget(createTimeHologram) ?: return@add
                        storage.setAndSyncToServer(target)
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
                            addClientMessage("see entity to hide")
                        } else if (!Config.Client.hideEntityTypes.get().contains(entry)) {
                            Config.Client.hideEntityTypes.modifyAndSave {
                                it += entry
                            }
                            addClientMessage("hide entity $entry")
                            HologramManager.checkAllHologramByPrevent()
                        } else {
                            Config.Client.hideEntityTypes.modifyAndSave {
                                it -= entry
                            }
                            addClientMessage("not hide entity $entry")
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
                            addClientMessage("see block to hide")
                        } else if (!Config.Client.hideBlocks.get().contains(entry)) {
                            Config.Client.hideBlocks.modifyAndSave {
                                it += entry
                            }
                            addClientMessage("hide Block $entry")
                            HologramManager.checkAllHologramByPrevent()
                        } else {
                            Config.Client.hideBlocks.modifyAndSave {
                                it -= entry
                            }
                            addClientMessage("not hide Block $entry")
                        }
                    }
                }
            }
            addGroup(ComponentRenderElement("debug options").setScale(0.8)) {
                addOption(Config.Client.renderDebugLayer, "debugLayer")
                addOption(Config.Client.renderDebugHologramLifeCycleBox, "hologramLifeCycle")
                addOption(Config.Client.renderWidgetDebugInfo, "widgetDebugInfo")
                addOption(Config.Client.renderNetworkDebugInfo, "networkDebugInfo")
                addOption(Config.Client.renderDebugTransientTarget, "transientTarget")
                addOption(Config.Client.renderInteractTransientReMappingIndicator, "remappingIndicator")
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

    private fun checkBlockContains(block: Block) =
        Config.Client.hideBlocks.get().contains(block.location().toString())

    private fun checkEntityContains(entity: EntityType<*>) =
        Config.Client.hideEntityTypes.get().contains(entity.location().toString())
}
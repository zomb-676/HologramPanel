package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.interaction.InteractionCommand.Exact
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand.Raw
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.InteractionLayer
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidget
import org.lwjgl.glfw.GLFW

object InteractionModeManager {
    enum class Mode {
        DISABLE, HOLOGRAM, INTERACT;

        fun isDisable(): Boolean = this == DISABLE
    }

    var mode: Mode = Mode.DISABLE
        private set(value) {
            field = value
            when (value) {
                Mode.DISABLE -> {
                    this.selectedHologram = null
                    this.findCandidateHologram = null
                }

                Mode.HOLOGRAM -> {
                    this.selectedHologram = null
                }

                Mode.INTERACT -> {
                    this.findCandidateHologram = null
                }
            }
        }

    private var selectedHologram: HologramRenderState? = null
        set(value) {
            field?.widget?.onDisSelected()
            field = value
            field?.widget?.onSelected()
        }

    private var findCandidateHologram: HologramRenderState? = null

    fun onRawCommand(raw: Raw) {
        val lookingTarget = HologramManager.getLookingHologram()
        val exactCommand: Exact? = when (mode) {
            Mode.DISABLE -> throw RuntimeException()
            Mode.HOLOGRAM -> {
                when (raw) {
                    is Raw.Key ->
                        if (raw.action == GLFW.GLFW_PRESS) {
                            when (raw.key) {
                                GLFW.GLFW_KEY_UP -> Exact.SelectHologram.SWITCH_HOLOGRAM_UP
                                GLFW.GLFW_KEY_DOWN -> Exact.SelectHologram.SWITCH_HOLOGRAM_DOWN
                                GLFW.GLFW_KEY_LEFT -> Exact.SelectHologram.SWITCH_HOLOGRAM_LEFT
                                GLFW.GLFW_KEY_RIGHT -> Exact.SelectHologram.SWITCH_HOLOGRAM_RIGHT
                                else -> null
                            }
                        } else null

                    is Raw.MouseButton -> if (raw.action == GLFW.GLFW_PRESS) {
                        when (raw.button) {
                            GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                                Exact.SelectHologram.SELECT_HOLOGRAM
                            }

                            else -> null
                        }
                    } else null

                    is Raw.MouseMove -> null
                    is Raw.MouseScroll -> when {
                        raw.scrollDeltaY > 0 -> Exact.SelectHologram.SWITCH_HOLOGRAM_BEFORE
                        raw.scrollDeltaY < 0 -> Exact.SelectHologram.SWITCH_HOLOGRAM_NEXT
                        else -> null
                    }
                }
            }

            Mode.INTERACT -> {
                when (raw) {
                    is Raw.Key -> if (raw.action == GLFW.GLFW_PRESS) {
                        when (raw.key) {
                            GLFW.GLFW_KEY_TAB -> Exact.OperateCommand.SWITCH_COLLAPSE
                            else -> if (this.shouldRestPlayerClientInput()) {
                                when (raw.key) {
                                    GLFW.GLFW_KEY_W -> Exact.SelectComponent.SELECT_BEFORE
                                    GLFW.GLFW_KEY_S -> Exact.SelectComponent.SELECT_NEXT
                                    GLFW.GLFW_KEY_A -> Exact.SelectComponent.SELECT_PARENT
                                    GLFW.GLFW_KEY_D -> Exact.SelectComponent.SELECT_GROUP_FIRST_CHILD
                                    else -> null
                                }
                            } else null
                        }
                    } else null

                    is Raw.MouseButton -> if (raw.action == GLFW.GLFW_PRESS) {
                        when(raw.button) {
                            GLFW.GLFW_MOUSE_BUTTON_LEFT -> Exact.OperateCommand.SWITCH_COLLAPSE
                            else -> null
                        }
                    } else null
                    is Raw.MouseMove -> null
                    is Raw.MouseScroll -> when {
                        raw.scrollDeltaY > 0 -> Exact.SelectComponent.SELECT_BEFORE
                        raw.scrollDeltaY < 0 -> Exact.SelectComponent.SELECT_NEXT
                        else -> null
                    }
                }
            }
        }

        InteractionLayer.updateExactCommand(exactCommand)
        this.processCommand(raw, exactCommand, lookingTarget)
    }

    private fun processCommand(raw: Raw, exact: Exact?, lookingTarget: HologramRenderState?) {
        when (exact) {
            is Exact.SelectHologram -> {
                when (exact) {
                    Exact.SelectHologram.SELECT_HOLOGRAM -> {
                        this.selectedHologram = this.findCandidateHologram ?: lookingTarget ?: return
                        this.mode = Mode.INTERACT
                        return
                    }

                    Exact.SelectHologram.SWITCH_HOLOGRAM_UP -> {}
                    Exact.SelectHologram.SWITCH_HOLOGRAM_RIGHT -> {}
                    Exact.SelectHologram.SWITCH_HOLOGRAM_DOWN -> {}
                    Exact.SelectHologram.SWITCH_HOLOGRAM_LEFT -> {}
                    Exact.SelectHologram.UNSELECT -> {
                        this.selectedHologram = null
                        this.mode = Mode.HOLOGRAM
                    }

                    Exact.SelectHologram.SWITCH_HOLOGRAM_BEFORE, Exact.SelectHologram.SWITCH_HOLOGRAM_NEXT -> {
                        this.findCandidateHologram =
                            HologramManager.getSubsequentDisplayedCandidate(this.findCandidateHologram, exact)
                    }
                }
            }

            is Exact.SelectComponent -> {
                val select = this.selectedHologram ?: return
                val widget = select.widget
                if (widget is HologramComponentWidget<*>) {
                    widget.selectComponent(select, exact)
                }
                return
            }

            is Exact.OperateCommand -> {
                when (exact) {
                    Exact.OperateCommand.SWITCH_COLLAPSE -> {
                        val select = this.selectedHologram ?: return
                        val widget = select.widget
                        if (widget is HologramComponentWidget<*>) {
                            widget.operateCommand(select, exact)
                        }
                        return
                    }
                }
            }

            else -> {}
        }
    }

    fun switchModeKeyToggled() {
        this.mode = when (this.mode) {
            Mode.DISABLE -> Mode.HOLOGRAM
            Mode.HOLOGRAM -> Mode.DISABLE
            Mode.INTERACT -> Mode.HOLOGRAM
        }
    }

    fun getSelectedHologram() = this.selectedHologram

    fun getFindCandidateHologram() = this.findCandidateHologram

    fun shouldRestPlayerClientInput(): Boolean {
        val select = this.getSelectedHologram()
        val looking = HologramManager.getLookingHologram()
        return select != null && select == looking
    }

    fun shouldPreventPauseGame(): Boolean {
        return shouldRestPlayerClientInput()
    }

    fun clearState() {
        this.mode = Mode.DISABLE
    }

    fun onWidgetRemoved(widget: HologramWidget) {
        if (this.selectedHologram?.widget == widget) {
            this.selectedHologram = null
        }
        if (this.findCandidateHologram?.widget == widget) {
            this.findCandidateHologram = null
        }
    }
}
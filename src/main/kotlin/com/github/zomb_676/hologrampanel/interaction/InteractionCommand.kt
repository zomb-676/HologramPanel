package com.github.zomb_676.hologrampanel.interaction

import net.neoforged.neoforge.client.event.InputEvent

sealed interface InteractionCommand {
    sealed interface Raw : InteractionCommand {
        fun post() {
            InteractionModeManager.onRawCommand(this)
        }

        data class Key(
            val key: Int, val scanCode: Int, val action: Int, val modifiers: Int
        ) : Raw {
            companion object {
                fun create(event: InputEvent.Key): Key {
                    return Key(event.key, event.scanCode, event.action, event.modifiers)
                }
            }
        }

        data class MouseScroll(
            val scrollDeltaX: Double,
            val scrollDeltaY: Double,
            val mouseX: Double,
            val mouseY: Double,
            val leftDown: Boolean,
            val middleDown: Boolean,
            val rightDown: Boolean,
        ) : Raw {
            companion object {
                fun create(event: InputEvent.MouseScrollingEvent): MouseScroll {
                    return MouseScroll(
                        event.scrollDeltaX,
                        event.scrollDeltaY,
                        event.mouseX,
                        event.mouseY,
                        event.isLeftDown,
                        event.isMiddleDown,
                        event.isRightDown
                    )
                }
            }
        }

        data class MouseButton(
            val button: Int,
            val action: Int,
            val modifiers: Int,
        ) : Raw {
            companion object {
                fun create(event: InputEvent.MouseButton): MouseButton {
                    return MouseButton(event.button, event.action, event.modifiers)
                }
            }
        }

        data class MouseMove(val x: Int, val y: Int) : Raw
    }

    sealed interface Exact : InteractionCommand {
        enum class CycleDetail : Exact {
            CYCLE_DISPLAY_DETAIL_MORE, CYCLE_DISPLAY_DETAIL_LESS,
        }

        enum class SelectHologram : Exact {
            SELECT_HOLOGRAM, SWITCH_HOLOGRAM_UP, SWITCH_HOLOGRAM_RIGHT, SWITCH_HOLOGRAM_DOWN, SWITCH_HOLOGRAM_LEFT, SWITCH_HOLOGRAM_BEFORE, SWITCH_HOLOGRAM_NEXT, UNSELECT
        }

        enum class OperateCommand : Exact {
            SWITCH_COLLAPSE
        }
    }
}
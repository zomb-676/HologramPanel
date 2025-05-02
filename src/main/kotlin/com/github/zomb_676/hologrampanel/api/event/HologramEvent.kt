package com.github.zomb_676.hologrampanel.api.event

import com.github.zomb_676.hologrampanel.api.TicketAdder
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.InteractiveEntry
import com.github.zomb_676.hologrampanel.util.dispatchForge
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import net.minecraft.network.chat.Component

abstract class HologramEvent<T : HologramContext>(private val state: HologramRenderState) : IHologramEvent() {
    fun getHologramWidget(): HologramWidget = state.widget
    fun getHologramState(): HologramRenderState = state

    fun getContext(): T = state.context.unsafeCast()

    /**
     * this is called before truly add a hologram
     */
    class Add<T : HologramContext>(state: HologramRenderState) : HologramEvent<T>(state) {
        private var allowAdd: Boolean = true

        fun allowAdd(): Boolean = allowAdd
        fun setAllowAdd(allow: Boolean) {
            this.allowAdd = allow
        }
    }

    /**
     * this is called when a hologram is about to be removed
     * call [getTicketAdder] and add ticket to renew it
     */
    class Remove<T : HologramContext>(state: HologramRenderState) : HologramEvent<T>(state) {
        private var ticketAdder = TicketAdder<T>(mutableListOf())
        fun allowRemove(): Boolean = this.ticketAdder.isEmpty()

        fun getTicketAdder(): TicketAdder<T> = ticketAdder
    }

    /**
     * this is called to check if interact should happen
     */
    sealed class Interact<T : HologramContext>(state: HologramRenderState) : HologramEvent<T>(state) {
        protected var interactMessage: Component? = null
            private set

        fun allowInteract(): Boolean = this.interactMessage == null

        fun interactMessage() = this.interactMessage

        fun setPreventInteractMessage(message: Component) {
            this.interactMessage = message
        }

        fun setAllowInteract() {
            this.interactMessage = null
        }

        class Key<T : HologramContext>(state: HologramRenderState, val key: HologramInteractionManager.Key) : Interact<T>(state) {
            companion object {
                fun checkAllow(key: HologramInteractionManager.Key): Interact<HologramContext>? {
                    val target = HologramManager.getInteractHologram() ?: return null
                    return Key<HologramContext>(target, key).dispatchForge()
                }
            }
        }

        class MouseClicked<T : HologramContext>(state: HologramRenderState, val button: HologramInteractionManager.MouseButton) : Interact<T>(state) {
            companion object {
                fun checkAllow(button: HologramInteractionManager.MouseButton): Interact<HologramContext>? {
                    val target = HologramManager.getInteractHologram() ?: return null
                    return MouseClicked<HologramContext>(target, button).dispatchForge()
                }
            }
        }

        class MouseScroll<T : HologramContext>(state: HologramRenderState, val scroll: HologramInteractionManager.MouseScroll) : Interact<T>(state) {
            companion object {
                fun checkAllow(scroll: HologramInteractionManager.MouseScroll): Interact<HologramContext>? {
                    val target = HologramManager.getInteractHologram() ?: return null
                    return MouseScroll<HologramContext>(target, scroll).dispatchForge()
                }
            }
        }

        class MouseDrag<T : HologramContext>(
            state: HologramRenderState,
            val interactive: InteractiveEntry,
            val dragContext: HologramInteractionManager.DragDataContext<*>
        ) : Interact<T>(state) {
            companion object {
                fun checkAllow(interactive: InteractiveEntry, dragContext: HologramInteractionManager.DragDataContext<*>?): Interact<HologramContext>? {
                    val target = HologramManager.getInteractHologram() ?: return null
                    val dragContext = dragContext ?: return null
                    return MouseDrag<HologramContext>(target, interactive, dragContext).dispatchForge()
                }
            }
        }
    }
}
package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.util.InteractiveEntry.Companion.FORBIDEN_COMPONENT
import net.minecraft.client.Minecraft
import net.neoforged.neoforge.client.event.InputEvent
import org.jetbrains.annotations.ApiStatus

object HologramInteractionManager {

    fun onMouseClick(event: InputEvent.MouseButton.Pre): Boolean {
        println(MouseButton.create(event))
        val interactiveTarget = HologramManager.getInteractiveTarget() ?: return false
        if (Config.Server.allowHologramInteractive.get()) {
            val player = Minecraft.getInstance().player ?: return false
            val data = MouseButton.create(event)
            val res = interactiveTarget.onMouseClick(data, player)
            return res
        } else {
            Minecraft.getInstance().gui.setOverlayMessage(FORBIDEN_COMPONENT, false)
            return true
        }
    }

    fun onMouseScroll(event: InputEvent.MouseScrollingEvent): Boolean {
        println(MouseScroll.create(event))
        val interactiveTarget = HologramManager.getInteractiveTarget() ?: return false
        if (Config.Server.allowHologramInteractive.get()) {
            val player = Minecraft.getInstance().player ?: return false
            val mouseScroll = MouseScroll.create(event)
            return interactiveTarget.onMouseScroll(mouseScroll, player)
        } else {
            Minecraft.getInstance().gui.setOverlayMessage(FORBIDEN_COMPONENT, false)
            return true
        }
    }

    fun onKey(event: InputEvent.Key): Boolean {
        println(Key.create(event))
        val interactiveTarget = HologramManager.getInteractiveTarget() ?: return false
        if (Config.Server.allowHologramInteractive.get()) {
            val player = Minecraft.getInstance().player ?: return false
            val key = Key.create(event)
            return interactiveTarget.onKey(key, player)
        } else {
            Minecraft.getInstance().gui.setOverlayMessage(FORBIDEN_COMPONENT, false)
            return true
        }
    }

    /**
     * callback function packs to check state for the source validation and do post-transform
     */
    interface DragCallback<T : Any> {
        /**
         * @param remainData data consumed after [DragDataContext.consumeDrag]
         */
        fun processTransformRemain(remainData: T)

        /**
         * check the drag source is still valid or not
         *
         * item may have been removed by automatic machine or any other reasons
         *
         * @return false will interrupt the drag
         */
        fun dragSourceStillValid(): Boolean
    }

    /**
     * this class instance should be crated by the drag begin target, at [com.github.zomb_676.hologrampanel.api.HologramInteractive.onTrigDrag]
     *
     * @property dragData the data that will be used to check if another interactive can receive this
     */
    class DragDataContext<T : Any>(private val dragData: T, val callback: DragCallback<T>) {
        var dragDataConsumed = false
            private set

        /**
         * for the interactive that mouse release over you to call
         *
         * @param code return the remain data; in some cases, data can't be totally transformed
         * return null will skip [DragCallback.processTransformRemain]
         */
        fun consumeDrag(code: (data: T) -> T?) {
            if (!dragDataConsumed) {
                val remainData = code.invoke(dragData) ?: return
                this.callback.processTransformRemain(remainData)
            } else throw RuntimeException("don't consume drag data multi-times")
        }

        /**
         * get the [dragData], only available while not consumed
         */
        internal fun getDragData(): T {
            if (dragDataConsumed) throw RuntimeException("data already consumed")
            return dragData
        }

        /**
         * used to check if the drag data can be consumed or not
         */
        fun getDragDataClass(): Class<out T> = dragData::class.java

        inline fun <reified C> isDataOfType(): Boolean = getDragDataClass().isAssignableFrom(C::class.java)

        fun isStillValid() = callback.dragSourceStillValid()
    }

    data class Key(val key: Int, val scanCode: Int, val action: Int, val modifiers: Int) {
        companion object {
            @ApiStatus.Internal
            internal fun create(event: InputEvent.Key): Key = Key(event.key, event.scanCode, event.action, event.modifiers)
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
    ) {
        companion object {
            @ApiStatus.Internal
            internal fun create(event: InputEvent.MouseScrollingEvent): MouseScroll {
                return MouseScroll(
                    event.scrollDeltaX, event.scrollDeltaY, event.mouseX, event.mouseY, event.isLeftDown, event.isMiddleDown, event.isRightDown
                )
            }
        }

    }

    data class MouseButton(val button: Int, val action: Int, val modifiers: Int) {
        companion object {
            @ApiStatus.Internal
            internal fun create(event: InputEvent.MouseButton): MouseButton = MouseButton(event.button, event.action, event.modifiers)
        }
    }
}
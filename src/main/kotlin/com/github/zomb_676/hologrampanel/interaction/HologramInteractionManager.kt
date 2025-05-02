package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.api.event.HologramEvent
import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager.dragData
import com.github.zomb_676.hologrampanel.trans.TransHandle
import com.github.zomb_676.hologrampanel.trans.TransPath
import com.github.zomb_676.hologrampanel.trans.TransSource
import com.github.zomb_676.hologrampanel.util.InteractiveEntry
import com.github.zomb_676.hologrampanel.util.MouseInputModeUtil
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.client.event.InputEvent
import org.jetbrains.annotations.ApiStatus
import org.lwjgl.glfw.GLFW

object HologramInteractionManager {
    private val FORBIDDEN_COMPONENT = Component.literal("Interactive Is Disabled On This Server")
    private val MISSING_PREVENT_COMPONENT = Component.literal("Interactive Prevent Message missing")

    val mouseClicked get() = GLFW.glfwGetMouseButton(Minecraft.getInstance().window.window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != 0

    internal var draggingSource: InteractiveEntry? = null
        private set
    internal var dragData: DragDataContext<*>? = null
        private set

    /**
     * used to check if player's mouse change in level
     */
    private object ViewChecker {
        private var xRot: Float = Minecraft.getInstance().gameRenderer.mainCamera.xRot
        private var yRot: Float = Minecraft.getInstance().gameRenderer.mainCamera.yRot
        private var position: Vec3 = Minecraft.getInstance().gameRenderer.mainCamera.position

        fun checkChange(): Boolean {
            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            var change = false
            if (this.xRot != camera.xRot) {
                change = true
                this.xRot = camera.xRot
            }
            if (this.yRot != camera.yRot) {
                change = true
                this.yRot = camera.yRot
            }
            if (this.position != camera.position) {
                change = true
                this.position = camera.position
            }
            return change
        }

        override fun toString(): String {
            return "ViewChecker(xRot=$xRot, yRot=$yRot, position=$position)"
        }
    }

    /**
     * this should be called when current interactive is set
     */
    fun renderTick() {
        if (!MouseInputModeUtil.overlayMouseMove() && !ViewChecker.checkChange()) return
        if (!Config.Server.allowHologramInteractive.get()) return
        if (CycleSelector.instanceExist()) return
        val player = Minecraft.getInstance().player ?: return
        val currentInteractive = HologramManager.getInteractiveTarget()
        if (currentInteractive != null) {
            if (dragData == null && mouseClicked && draggingSource == null) {
                this.draggingSource = currentInteractive
                val data = currentInteractive.trigDrag(player) ?: return
                this.dragData = data
            } else if (dragData != null && draggingSource != null && draggingSource!!.getLatestInteractiveEntry() !== currentInteractive.getLatestInteractiveEntry()) {
                currentInteractive.onDragPass(dragData!!)
            }
        }
        if (dragData != null && draggingSource != null) {
            if (!dragData!!.isStillValid() || draggingSource!!.getLatestInteractiveEntry() == null) {
                dragData = null
                draggingSource = null
            }
        }
    }

    fun onMouseClick(event: InputEvent.MouseButton.Pre): Boolean {
        val interactiveTarget = HologramManager.getInteractiveTarget()
        if (interactiveTarget == null) {
            draggingSource = null
            dragData = null
            return false
        }
        if (!Config.Server.allowHologramInteractive.get()) {
            setOverlayMessage(FORBIDDEN_COMPONENT)
            return true
        }
        val mouseClicked = MouseButton.create(event)
        val checkEvent = HologramEvent.Interact.MouseClicked.checkAllow(mouseClicked) ?: return false
        if (checkEvent.allowInteract()) {
            if (event.action == GLFW.GLFW_RELEASE) {
                if (draggingSource != null && dragData != null && event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    if (draggingSource!!.getLatestInteractiveEntry() !== interactiveTarget.interactive) {
                        interactiveTarget.onDragTransform(dragData!!)
                        dragData = null
                        draggingSource = null
                        return true
                    }
                    dragData = null
                    draggingSource = null
                }
                val player = Minecraft.getInstance().player ?: return true
                val res = interactiveTarget.onMouseClick(mouseClicked, player)
                return res
            } else return true
        } else {
            setOverlayMessage(checkEvent.interactMessage() ?: MISSING_PREVENT_COMPONENT)
            return true
        }
    }

    fun onMouseScroll(event: InputEvent.MouseScrollingEvent): Boolean {
        val interactiveTarget = HologramManager.getInteractiveTarget() ?: return false
        if (!Config.Server.allowHologramInteractive.get()) {
            setOverlayMessage(FORBIDDEN_COMPONENT)
            return true
        }
        val player = Minecraft.getInstance().player ?: return true
        val mouseScroll = MouseScroll.create(event)
        val event = HologramEvent.Interact.MouseScroll.checkAllow(mouseScroll) ?: return true
        if (event.allowInteract()) {
            return interactiveTarget.onMouseScroll(mouseScroll, player)
        } else {
            setOverlayMessage(event.interactMessage() ?: MISSING_PREVENT_COMPONENT)
            return true
        }
    }

    fun onKey(event: InputEvent.Key): Boolean {
        val interactiveTarget = HologramManager.getInteractiveTarget() ?: return false
        if (!Config.Server.allowHologramInteractive.get()) {
            setOverlayMessage(FORBIDDEN_COMPONENT)
            return true
        }
        val player = Minecraft.getInstance().player ?: return true
        val key = Key.create(event)
        val event = HologramEvent.Interact.Key.checkAllow(key) ?: return true
        if (event.allowInteract()) {
            return interactiveTarget.onKey(key, player)
        } else {
            setOverlayMessage(event.interactMessage() ?: MISSING_PREVENT_COMPONENT)
            return true
        }
    }

    fun clearState() {
        this.draggingSource = null
        this.dragData = null
    }

    private fun setOverlayMessage(messages: Component) {
        Minecraft.getInstance().gui.setOverlayMessage(messages, false)
    }

    /**
     * callback function packs to check state for the source validation and do post-transform
     */
    interface DragCallback<T : Any> {
        /**
         * @param remainData data consumed after [DragDataContext.consumeDrag]
         */
        fun processTransformRemain(remainData: T) {}

        /**
         * check the drag source is still valid or not
         *
         * item may have been removed by automatic machine or any other reasons
         *
         * should consider call [com.github.zomb_676.hologrampanel.util.RebuildValue.getCurrent]
         *
         * @return false will interrupt the drag
         */
        fun dragSourceStillValid(): Boolean

        fun getDragData(): T?

        /**
         * when call is function, should guarantee the return value of [getDragData] is not null
         */
        fun <S : Any, H : Any> getTransInfo(): Triple<TransSource<S>, TransHandle<S, H>, TransPath<H, T>>? = null
    }

    /**
     * this class instance should be crated by the drag begin target, at [com.github.zomb_676.hologrampanel.api.HologramInteractive.onTrigDrag]
     *
     * @property dragData the data that will be used to check if another interactive can receive this
     */
    class DragDataContext<T : Any>(val callback: DragCallback<T>) {
        var dragDataConsumed = false
            private set
        private val type = getDragData()?.javaClass

        /**
         * for the interactive that mouse release over you to call
         *
         * @param code return the remain data; in some cases, data can't be totally transformed
         * return null will skip [DragCallback.processTransformRemain]
         */
        fun consumeDrag(code: (data: T) -> T?) {
            if (!dragDataConsumed) {
                val dragData = getDragData() ?: return
                val remainData = code.invoke(dragData) ?: return
                this.callback.processTransformRemain(remainData)
            } else throw RuntimeException("don't consume drag data multi-times")
        }

        inline fun <reified C> consumeTyped(noinline code: (data: C) -> C?) {
            if (isDataOfType<C>()) {
                consumeDrag(code.unsafeCast())
            }
        }

        /**
         * get the [dragData], only available while not consumed
         */
        internal fun getDragData(): T? {
            if (dragDataConsumed) throw RuntimeException("data already consumed")
            return callback.getDragData()
        }

        /**
         * used to check if the drag data can be consumed or not
         */
        fun getDragDataClass(): Class<out T>? = type

        inline fun <reified C> isDataOfType(): Boolean = getDragDataClass()?.isAssignableFrom(C::class.java) == true

        @Suppress("UNCHECKED_CAST")
        fun <C> getTypedDragData() = getDragData() as? C?

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

    data class MouseButton(val button: Int, val modifiers: Int) {
        companion object {
            @ApiStatus.Internal
            internal fun create(event: InputEvent.MouseButton): MouseButton = MouseButton(event.button, event.modifiers)
        }
    }
}
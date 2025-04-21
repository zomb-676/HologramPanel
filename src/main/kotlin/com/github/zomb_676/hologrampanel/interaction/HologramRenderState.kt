package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.HologramTicket
import com.github.zomb_676.hologrampanel.api.TicketAdder
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.JomlMath
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.mainCamera
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import net.minecraft.client.Minecraft
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.ceil
import kotlin.math.sqrt

class HologramRenderState(
    val widget: HologramWidget,
    val context: HologramContext,
    displayType: DisplayType,
    additionTicket: List<HologramTicket<*>>
) {
    /**
     * if the widget is rendered or not
     */
    var displayed: Boolean = false

    var locate: LocateType = LocateType.World.FacingPlayer

    /**
     * the size of the [widget], not influenced by [com.mojang.blaze3d.vertex.PoseStack.scale]
     */
    var size: Size = Size.ZERO

    /**
     * the size influenced by [displayScale] and [com.mojang.blaze3d.vertex.PoseStack], used for cursor detection
     *
     * coordinate is at minecraft screen space, can't indicate actual pixel size
     *
     * this can't be directly used during rendering HologramWidget, and the scale will be considered multi-times
     */
    var displaySize: Size = Size.ZERO

    /**
     * screen space position the widget should be rendered, anchored by widget's center
     */
    var screenPos: ScreenPosition = ScreenPosition.ZERO

    /**
     * current display scale, influenced by distance, [com.github.zomb_676.hologrampanel.Config.Client.globalHologramScale]
     */
    var displayScale: Double = 1.0
        private set

    var inViewDegree: Boolean = false
        private set

    /**
     * like [net.minecraft.world.entity.Entity.isRemoved]
     */
    var removed: Boolean = false
        internal set
    var displayType: DisplayType = displayType
        set(value) {
            if (value != field) {
                field = value
                if (widget is DynamicBuildWidget<*>) {
                    widget.updateComponent(value)
                }
            }
        }

    /**
     * all the [HologramTicket]
     */
    internal val hologramTicks: MutableList<HologramTicket<HologramContext>> = run {
        @Suppress("UNCHECKED_CAST")
        fun <T : HologramContext> f(context: T): MutableList<HologramTicket<T>> {
            val list: MutableList<HologramTicket<T>> = additionTicket.toMutableList().unsafeCast()
            val adder = TicketAdder(list)
            if (widget is DynamicBuildWidget<*>) {
                (widget.providers as List<ComponentProvider<T, *>>).forEach {
                    it.attachTicket(context.unsafeCast(), adder)
                }
            }
            return list
        }
        f(context)
    }

    /**
     * check [HologramContext.stillValid] and [HologramTicket]
     */
    fun stillValid(): Boolean {
        if (!context.stillValid()) return false
        if (hologramTicks.isEmpty()) return false
        hologramTicks.forEach { ticket ->
            val pass = ticket.stillValid(this.context, this)
            if (ticket.isCritical()) {
                if (!pass) return false
            } else {
                if (pass) return true
            }
        }
        return true
    }

    fun sourcePosition(partialTick: Float) = locate.getSourceWorldPosition(context, partialTick)

    /**
     * measure the size of the widget and record it
     */
    fun measure(displayType: DisplayType, hologramStyle: HologramStyle): Size {
        this.size = widget.measure(hologramStyle, displayType)
        return this.size
    }

    /**
     * via [MVPMatrixRecorder.transform], transforming world vec3 to [com.github.zomb_676.hologrampanel.util.packed.ScreenCoordinate]
     */
    fun updateRenderScreenPosition(partialTick: Float): ScreenPosition {
        this.screenPos = this.locate.getScreenSpacePosition(context, partialTick)
        return this.screenPos
    }

    fun getSourceScreenPosition(partialTick: Float): ScreenPosition {
        if (this.locate is LocateType.World) return this.screenPos
        return this.locate.getSourceScreenSpacePosition(context, partialTick)
    }

    /**
     * set the render scale of the widget
     */
    fun setDisplayScale(scale: Double) {
        this.displayScale = scale
    }

    /**
     * clip Hologram that is at the back of the view
     */
    fun viewVectorDegreeCheckPass(partialTick: Float): Boolean {
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val viewVector = camera.lookVector
        val cameraPosition = camera.position
        val sourcePosition = this.sourcePosition(partialTick)
        val sourceVector = Vector3f(
            (sourcePosition.x() - cameraPosition.x).toFloat(),
            (sourcePosition.y() - cameraPosition.y).toFloat(),
            (sourcePosition.z() - cameraPosition.z).toFloat()
        ).normalize()

        val dot = viewVector.dot(sourceVector)
        val angleInRadius = JomlMath.acos(dot)
        val angel = JomlMath.toDegrees(angleInRadius.toDouble())
        this.inViewDegree = angel < Minecraft.getInstance().options.fov().get()
        return if (this.locate is LocateType.World) this.inViewDegree else true
    }

    /**
     * update [displaySize]
     *
     * @return if the current widget has any part in the screen
     */
    fun displayAreaInScreen(matrix4f: Matrix4f): Boolean {
        val window = Minecraft.getInstance().window
        val width = window.guiScaledWidth.toFloat()
        val height = window.guiScaledHeight.toFloat()

        val checkVector = Vector4f(0f, 0f, 0f, 1f)
        matrix4f.transform(checkVector)
        val x1 = checkVector.x
        val y1 = checkVector.y

        checkVector.set(size.width.toFloat(), size.height.toFloat(), 0f, 1f)
        matrix4f.transform(checkVector)
        val x2 = checkVector.x
        val y2 = checkVector.y

        this.displaySize = Size.of(ceil(x2 - x1).toInt(), ceil(y2 - y1).toInt())

        return !(x1 > width || x2 < 0 || y1 > height || y2 < 0)
    }

    fun updateDisplaySize(matrix4f: Matrix4f) {
        val checkVector = Vector4f(0f, 0f, 0f, 1f)
        matrix4f.transform(checkVector)
        val x1 = checkVector.x
        val y1 = checkVector.y

        checkVector.set(size.width.toFloat(), size.height.toFloat(), 0f, 1f)
        matrix4f.transform(checkVector)
        val x2 = checkVector.x
        val y2 = checkVector.y

        this.displaySize = Size.of(ceil(x2 - x1).toInt(), ceil(y2 - y1).toInt())
    }

    /**
     * convenient function for check is interacted
     */
    fun isInteractAt() = HologramManager.getInteractHologram() == this

    /**
     * the distance between [mainCamera] and [sourcePosition]
     */
    fun distanceToCamera(partialTick: Float): Double {
        val source = this.sourcePosition(partialTick)
        val camera = Minecraft.getInstance().gameRenderer.mainCamera.position
        val x = source.x() - camera.x
        val y = source.y() - camera.y
        val z = source.z() - camera.z
        return sqrt(x * x + y * y + z * z)
    }
}
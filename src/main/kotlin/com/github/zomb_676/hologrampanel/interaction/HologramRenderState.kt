package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.HologramTicket
import com.github.zomb_676.hologrampanel.api.TicketAdder
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.*
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import net.minecraft.client.Minecraft
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.sqrt

class HologramRenderState(
    val widget: HologramWidget,
    val context: HologramContext,
    displayType: DisplayType,
    additionTicket: List<HologramTicket<*>>
) {
    var displayed: Boolean = false

    var size: Size = Size.ZERO
    var displaySize: Size = Size.ZERO
    var centerScreenPos: ScreenCoordinate = ScreenCoordinate.ZERO
    var displayScale: Double = 1.0
    var removed: Boolean = false
    var displayType: DisplayType = displayType
        set(value) {
            if (value != field) {
                field = value
                if (widget is DynamicBuildWidget<*>) {
                    widget.updateComponent(value)
                }
            }
        }

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

    fun sourcePosition(partialTick: Float) = context.hologramCenterPosition(partialTick)

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
        val angel = JomlMath.toDegrees(angleInRadius)
        val pass = angel < 80f
        this.displayed = pass
        return pass
    }

    fun measure(displayType: DisplayType, hologramStyle: HologramStyle): Size {
        this.size = widget.measure(hologramStyle, displayType)
        return this.size
    }

    fun updateScreenPosition(partialTick: Float): ScreenCoordinate {
        this.centerScreenPos = MVPMatrixRecorder.transform(this.sourcePosition(partialTick))
        return this.centerScreenPos
    }

    fun setDisplaySize(scale: Double) {
        val width = size.width * scale
        val height = size.height * scale
        this.displayScale = scale
        this.displaySize = Size.of(ceil(width).toInt(), ceil(height).toInt())
    }

    fun displayAreaInScreen() = this.centerScreenPos.inScreen(this.displaySize)

    fun isLookingAt() = HologramManager.getLookingHologram() == this

    fun distanceToCamera(partialTick: Float): Double {
        val source = this.sourcePosition(partialTick)
        val camera = Minecraft.getInstance().gameRenderer.mainCamera.position
        val x = source.x() - camera.x
        val y = source.y() - camera.y
        val z = source.z() - camera.z
        return sqrt(x * x + y * y + z * z)
    }
}
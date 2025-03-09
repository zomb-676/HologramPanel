package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.JomlMath
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.ScreenCoordinate
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import net.minecraft.client.Minecraft
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.sqrt

class HologramRenderState(val widget: HologramWidget, val context: HologramContext) {
    var displayed: Boolean = false

    var size: Size = Size.ZERO
    var displaySize: Size = Size.ZERO
    var centerScreenPos: ScreenCoordinate = ScreenCoordinate.ZERO
    var displayScale: Double = 1.0

    fun sourcePosition() = context.hologramCenterPosition()

    fun viewVectorDegreeCheckPass(): Boolean {
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val viewVector = camera.lookVector
        val cameraPosition = camera.position
        val sourcePosition = this.sourcePosition()
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

    fun updateScreenPosition(): ScreenCoordinate {
        this.centerScreenPos = MVPMatrixRecorder.transform(this.sourcePosition())
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
    fun isSelected() = InteractionModeManager.getSelectedHologram() == this

    fun distanceToCamera(): Double {
        val source = this.sourcePosition()
        val camera = Minecraft.getInstance().gameRenderer.mainCamera.position
        val x = source.x() - camera.x
        val y = source.y() - camera.y
        val z = source.z() - camera.z
        return sqrt(x * x + y * y + z * z)
    }
}
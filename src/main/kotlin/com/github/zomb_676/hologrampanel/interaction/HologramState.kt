package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.JomlMath
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.ScreenCoordinate
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import org.joml.Vector3f
import kotlin.math.ceil

class HologramState(val widget: HologramWidget, val sourcePos: BlockPos) {
    operator fun component1() = widget
    operator fun component2() = sourcePos
    var displayed: Boolean = false

    var size: Size = Size.ZERO
    var displaySize: Size = Size.ZERO
    var centerScreenPos: ScreenCoordinate = ScreenCoordinate.ZERO
    var displayScale: Double = 1.0

    fun viewVectorDegreeCheckPass(): Boolean {
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val viewVector = camera.lookVector
        val cameraPosition = camera.position
        val sourceVector = Vector3f(
            (sourcePos.x + 0.5 - cameraPosition.x).toFloat(),
            (sourcePos.y + 0.5 - cameraPosition.y).toFloat(),
            (sourcePos.z + 0.5 - cameraPosition.z).toFloat()
        ).normalize()

        val dot = viewVector.dot(sourceVector)
        val angleInRadius = JomlMath.acos(dot)
        val angel = JomlMath.toDegrees(angleInRadius)
        val pass = angel < 80f
        this.displayed = pass
        return pass
    }

    fun measure(displayType: HologramWidget.DisplayType, hologramStyle: HologramStyle): Size {
        this.size = widget.measure(displayType, hologramStyle)
        return this.size
    }

    fun updateScreenPosition(): ScreenCoordinate {
        this.centerScreenPos = MVPMatrixRecorder.transformBlockToOffset(sourcePos)
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

    fun translateToLeftUp(hologramStyle: HologramStyle) {
//        val pos = this.centerScreenPos.equivalentSmooth(hologramStyle)
//        hologramStyle.move(pos.x, pos.y)
//
//
//        val size = this.displaySize
//        hologramStyle.scale(this.displayScale, this.displayScale)
//        hologramStyle.translate(-size.width / 2.0f, -size.height / 2.0f)
    }
}
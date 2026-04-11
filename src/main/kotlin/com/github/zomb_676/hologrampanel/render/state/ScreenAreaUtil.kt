package com.github.zomb_676.hologrampanel.render.state

import net.minecraft.client.gui.navigation.ScreenRectangle
import org.joml.Matrix3x2fc
import kotlin.math.ceil

object ScreenAreaUtil {
    fun getBounds(x0: Float, y0: Float, x1: Float, y1: Float, pose: Matrix3x2fc, scissorArea: ScreenRectangle?): ScreenRectangle {
        val bounds = ScreenRectangle(x0.toInt(), y0.toInt(), ceil(x1 - x0).toInt(), ceil(y1 - y0).toInt()).transformMaxBounds(pose)
        return scissorArea?.intersection(bounds) ?: bounds
    }

    fun getBounds(x0: Double, y0: Double, x1: Double, y1: Double, pose: Matrix3x2fc, scissorArea: ScreenRectangle?): ScreenRectangle {
        return getBounds(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), pose, scissorArea)
    }
}
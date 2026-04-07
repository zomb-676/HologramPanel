package com.github.zomb_676.hologrampanel.util.renderstate

import net.minecraft.client.gui.navigation.ScreenRectangle
import org.joml.Matrix3x2fc
import kotlin.math.ceil

object ScreenAreaUtil {
    fun getBounds(x0: Float, y0: Float, x1: Float, y1: Float, pose: Matrix3x2fc, scissorArea: ScreenRectangle?): ScreenRectangle {
        val bounds = ScreenRectangle(x0.toInt(), y0.toInt(), ceil(x1 - x0).toInt(), ceil(y1 - y0).toInt()).transformMaxBounds(pose)
        return scissorArea?.intersection(bounds) ?: bounds
    }
}
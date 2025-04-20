package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.mojang.blaze3d.platform.Window
import net.minecraft.client.Minecraft
import net.minecraft.client.MouseHandler
import org.joml.*
import org.lwjgl.glfw.GLFW
import kotlin.math.abs

/**
 * get the correct mouse position in gui and the world
 *
 * support re-anchor mouse position
 *
 * use destructuring to get current mouse position
 *
 * ```kotlin
 * val (mouseX, mouseY) = MousePositionManager
 * ```
 */
object MousePositionManager {
    /**
     * resolve the `Homography Matrix` that used to remapping mouse position in for [LocateType.World.FacingVector]
     */
    private object HomographyMatrixResolver {
        /**
         * resolve the `Homography Matrix`
         *
         * @param srcLeftUp source input point, on minecraft screen space
         * @param srcLeftDown source input point, on minecraft screen space
         * @param srcRightDown source input point, on minecraft screen space
         * @param srcRightUp source input point, on minecraft screen space
         * @param width width of [com.github.zomb_676.hologrampanel.interaction.HologramRenderState.size]
         * @param height height of [com.github.zomb_676.hologrampanel.interaction.HologramRenderState.size]
         */
        private fun computeHomography(
            srcLeftUp: Vector2fc,
            srcLeftDown: Vector2fc,
            srcRightDown: Vector2fc,
            srcRightUp: Vector2fc,
            width: Float,
            height: Float
        ): Matrix3f {
            //target point position
            //should follow order or input a,b,c,d
            val dstLeftUp = Vector2f(0f, 0f)
            val dstLeftDown = Vector2f(0f, height)
            val dstRightDown = Vector2f(width, height)
            val dstRightUp = Vector2f(width, 0f)

            //build the linear equation group
            val a = Array(8) { FloatArray(8) }
            val bVec = FloatArray(8)

            //fill data
            fillEquationRow(a, bVec, row = 0, src = srcLeftUp, dst = dstLeftUp)
            fillEquationRow(a, bVec, row = 2, src = srcLeftDown, dst = dstLeftDown)
            fillEquationRow(a, bVec, row = 4, src = srcRightDown, dst = dstRightDown)
            fillEquationRow(a, bVec, row = 6, src = srcRightUp, dst = dstRightUp)

            //resolve the liner equation group
            val x = solveLinearSystem(a, bVec)

            //construct the HomographyMatrix by the resolution
            return Matrix3f().apply {
                m00(x[0])
                m10(x[3])
                m20(x[6])
                m01(x[1])
                m11(x[4])
                m21(x[7])
                m02(x[2])
                m12(x[5])
                m22(1f)
            }
        }

        /**
         * helper method to fill the linear equation group
         *
         * @param src position from original space
         * @param dst position of target space, the mapped result of [src]
         */
        private fun fillEquationRow(a: Array<FloatArray>, b: FloatArray, row: Int, src: Vector2fc, dst: Vector2f) {
            val x = src.x()
            val y = src.y()
            val u = dst.x
            val v = dst.y

            a[row][0] = x
            a[row][1] = y
            a[row][2] = 1f
            a[row][3] = 0f
            a[row][4] = 0f
            a[row][5] = 0f
            a[row][6] = -u * x
            a[row][7] = -u * y
            b[row] = u

            a[row + 1][0] = 0f
            a[row + 1][1] = 0f
            a[row + 1][2] = 0f
            a[row + 1][3] = x
            a[row + 1][4] = y
            a[row + 1][5] = 1f
            a[row + 1][6] = -v * x
            a[row + 1][7] = -v * y
            b[row + 1] = v
        }

        /**
         * use `Gaussian elimination with column pivoting` to solve the linear equation group
         *
         * solve `Ax=b`
         *
         * @param a coefficient vector
         * @param b constant vector
         * @return solution matrix data
         */
        private fun solveLinearSystem(a: Array<FloatArray>, b: FloatArray): FloatArray {
            val n = b.size
            val aug = Array(n) { i ->
                FloatArray(n + 1) { j -> if (j < n) a[i][j] else b[i] }
            }

            for (i in 0 until n) {
                //column pivoting
                var maxRow = i
                for (k in i until n) {
                    if (abs(aug[k][i]) > abs(aug[maxRow][i])) {
                        maxRow = k
                    }
                }
                val temp = aug[i]
                aug[i] = aug[maxRow]
                aug[maxRow] = temp

                //the pivot element can't be zero
                val pivot = aug[i][i]
                require(abs(pivot) > 1e-6f) {
                    "Matrix is singular, check if points are collinear"
                }

                //normalize current line
                for (j in i..n) aug[i][j] /= pivot

                //process the next line
                for (k in i + 1 until n) {
                    val factor = aug[k][i]
                    for (j in i..n) {
                        aug[k][j] -= factor * aug[i][j]
                    }
                }
            }

            //calculate the result
            val x = FloatArray(n)
            for (i in n - 1 downTo 0) {
                x[i] = aug[i][n]
                for (j in i + 1 until n) {
                    x[i] -= aug[i][j] * x[j]
                }
            }
            return x
        }

        /**
         * @param mouseX use minecraft screen space coordinate
         * @param mouseY use minecraft screen space coordinate
         *
         * @return the coordiante in anchored in left-up as original point
         * can be converted to minecraft screen space via a simple plus/minus transformation
         */
        fun solvePosition(mouseX: Float, mouseY: Float, size: Size, locate: LocateType.World.FacingVector): ScreenPosition {
            val matrix = computeHomography(
                locate.getLeftUp(), locate.getLeftDown(), locate.getRightDown(), locate.getRightUp(),
                size.width.toFloat(), size.height.toFloat()
            )
            val p = ScreenPosition.of(mouseX, mouseY)
            val w = matrix.m20 * p.x + matrix.m21 * p.y + matrix.m22
            val x = (matrix.m00 * p.x + matrix.m01 * p.y + matrix.m02) / w
            val y = (matrix.m10 * p.x + matrix.m11 * p.y + matrix.m12) / w
            return ScreenPosition.of(x, y)
        }
    }

    private var mouseX: Float = 0f
    private var mouseY: Float = 0f

    operator fun component1() = mouseX
    operator fun component2() = mouseY

    /**
     * update interact mouse position for [GLFW.GLFW_CURSOR] queried from [GLFW.glfwGetInputMode]
     *
     * when [GLFW.GLFW_CURSOR_NORMAL], it is in gui with mouse rendered and can be freely moved.
     * so use [MouseHandler.handleAccumulatedMovement] way to transform
     * glfw's mouse position space into minecraft screen space
     *
     * when [GLFW.GLFW_CURSOR_DISABLED], it is in level with no gui opened and mouse hidden.
     * in this case, [MouseHandler.xpos] and [MouseHandler.ypos] is not accurate, so
     * we use half of [Window.guiScaledWidth] and [Window.guiScaledHeight]
     */
    fun updateMousePosition(): MousePositionManager {
        val window: Window = Minecraft.getInstance().window
        when (GLFW.glfwGetInputMode(window.window, GLFW.GLFW_CURSOR)) {
            GLFW.GLFW_CURSOR_NORMAL -> {
                val mouseHandle: MouseHandler = Minecraft.getInstance().mouseHandler
                mouseX = (mouseHandle.xpos() * window.guiScaledWidth / window.screenWidth).toFloat()
                mouseY = (mouseHandle.ypos() * window.guiScaledHeight / window.screenHeight).toFloat()
            }

            GLFW.GLFW_CURSOR_DISABLED -> {
                mouseX = window.guiScaledWidth / 2.0f
                mouseY = window.guiScaledHeight / 2.0f
            }
        }
        return this
    }

    /**
     * remapping the mouse position
     */
    fun remappingMousePositionScope(mouseX: Float, mouseY: Float, code: () -> Unit) {
        val storeX = this.mouseX
        val storeY = this.mouseY
        this.mouseX = mouseX
        this.mouseY = mouseY
        code()
        this.mouseX = storeX
        this.mouseY = storeY
    }

    /**
     * current coordinate should be based on the left-up corner as its orign point
     */
    fun remappingMouseForLooking(state: HologramRenderState, code: () -> Unit) {
        val locate = state.locate as? LocateType.World.FacingVector ?: return
        val (mouseX, mouseY) = HomographyMatrixResolver.solvePosition(mouseX, mouseY, state.displaySize, locate)
        remappingMousePositionScope(mouseX, mouseY, code)
    }

    /**
     * re-anchor origin point by the origin of [matrix4f], should use same scale
     */
    fun relocateOriginPoint(matrix4f: Matrix4f, code: () -> Unit) {
        val anchor = matrix4f.transformPosition(0f, 0f, 0f, Vector3f())
        remappingMousePositionScope(this.mouseX + anchor.x, this.mouseY + anchor.y, code)
    }

    fun mouseInvalidAreaScope(code: () -> Unit) {
        remappingMousePositionScope(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, code)
    }
}
package com.github.zomb_676.hologrampanel.widget.locateType

import com.github.zomb_676.hologrampanel.PanelOperatorManager
import com.github.zomb_676.hologrampanel.util.renderstate.FloatColoredRectangleRenderState.Companion.fill
import com.github.zomb_676.hologrampanel.util.JomlMath
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.color.HologramColor
import com.github.zomb_676.hologrampanel.util.renderstate.FloatColoredQuadrilateralRenderState.Companion.fillQuad
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.client.gui.GuiLayer
import org.joml.*
import org.lwjgl.glfw.GLFW

enum class TransformOrientation {
    WORLD {
        override fun extractX(player: Player, local: Any): Vector3f = Vector3f(1f, 0f, 0f)
        override fun extractY(player: Player, local: Any): Vector3f = Vector3f(0f, 1f, 0f)
        override fun extractZ(player: Player, local: Any): Vector3f = Vector3f(0f, 0f, 1f)

        override fun rotateX(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf =
            rotation.rotateLocalX(modifyDegree)

        override fun rotateY(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf =
            rotation.rotateLocalY(modifyDegree)

        override fun rotateZ(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf =
            rotation.rotateLocalZ(modifyDegree)
    },
    LOCAL {
        override fun extractX(player: Player, local: Any): Vector3f? = (local as? LocateFreelyInWorld)?.getLeft()?.normalize(Vector3f())
        override fun extractY(player: Player, local: Any): Vector3f? = (local as? LocateFreelyInWorld)?.getUp()?.normalize(Vector3f())
        override fun extractZ(player: Player, local: Any): Vector3f? = (local as? LocateFreelyInWorld)?.getView()?.normalize(Vector3f())

        override fun rotateX(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf = rotation.rotateX(modifyDegree)
        override fun rotateY(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf = rotation.rotateY(modifyDegree)
        override fun rotateZ(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf = rotation.rotateZ(modifyDegree)
    },
    PLAYER {
        private inline val camera get() = Minecraft.getInstance().gameRenderer.mainCamera
        override fun extractX(player: Player, local: Any): Vector3f = camera.leftVector().normalize(Vector3f())
        override fun extractY(player: Player, local: Any): Vector3f = camera.upVector().normalize(Vector3f())
        override fun extractZ(player: Player, local: Any): Vector3f = camera.forwardVector().normalize(Vector3f())
        override fun rotateX(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf {
            val container = Quaternionf()
            return rotation.premul(cameraRotation.invert(container))
                .premul(cameraRotation.rotateX(modifyDegree, container))
        }

        override fun rotateY(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf {
            val container = Quaternionf()
            return rotation.premul(cameraRotation.invert(container))
                .premul(cameraRotation.rotateY(modifyDegree, container))
        }

        override fun rotateZ(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf {
            val container = Quaternionf()
            return rotation.premul(cameraRotation.invert(container))
                .premul(cameraRotation.rotateZ(modifyDegree, container))
        }
    };

    /**
     * the returned value can be modified safely
     */
    abstract fun extractX(player: Player, local: Any): Vector3f?

    /**
     * the returned value can be modified safely
     */
    abstract fun extractY(player: Player, local: Any): Vector3f?

    /**
     * the returned value can be modified safely
     */
    abstract fun extractZ(player: Player, local: Any): Vector3f?

    /**
     * @param rotation origin quaternion
     * @param cameraRotation quaternion used by camera
     * @param modifyDegree in radians
     * @return apply specific rotation to [rotation]
     */
    abstract fun rotateX(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf

    /**
     * @param rotation origin quaternion
     * @param cameraRotation quaternion used by camera
     * @param modifyDegree in radians
     * @return apply specific rotation to [rotation]
     */
    abstract fun rotateY(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf

    /**
     * @param rotation origin quaternion
     * @param cameraRotation quaternion used by camera
     * @param modifyDegree in radians
     * @return apply specific rotation to [rotation]
     */
    abstract fun rotateZ(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf

    companion object {
        private const val X_COLOR = 0xffff0000.toInt()
        private const val Y_COLOR = 0xff00ff00.toInt()
        private const val Z_COLOR = 0xff0000ff.toInt()

        fun getTransformOrientationLayer(): GuiLayer = object : GuiLayer {
            private fun addLine(
                begin: ScreenPosition,
                end: ScreenPosition,
                guiGraphics: GuiGraphicsExtractor,
                color: Int,
                width: Float = 2.0f
            ) {
                val vector = Vector2f(begin.y - end.y, end.x - begin.x).normalize().mul(width / 2)
                guiGraphics.fillQuad(begin.x + vector.x,
                    begin.y + vector.y,
                    end.x + vector.x,
                    end.y + vector.y,
                    end.x - vector.x, end.y - vector.y,
                    begin.x - vector.x, begin.y - vector.y,
                    color
                )
            }

            private fun changeAlpha(originColor: Int, key: Int): Int {
                val window = Minecraft.getInstance().window.handle()
                return if (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
                    originColor
                } else {
                    HologramColor.argb(originColor).alpha(0x7f).color
                }
            }

            override fun render(guiGraphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
                val target = PanelOperatorManager.selectedTarget ?: return
                val locate = target.locate as? LocateInWorld ?: return

                val worldPosition = target.getRenderWorldPosition()
                if (!viewVectorDegreeCheckPass(deltaTracker.getGameTimeDeltaPartialTick(false), worldPosition)) return

                val center = target.widgetScreenPos

                val transform = Matrix4f().translate(worldPosition.x(), worldPosition.y(), worldPosition.z()).apply {
                    when (PanelOperatorManager.transformOrientation) {
                        WORLD -> {}
                        LOCAL if locate is LocateFreelyInWorld -> rotate(locate.getMutableRotation())
                        PLAYER -> rotate(Minecraft.getInstance().gameRenderer.mainCamera.rotation())
                        else -> return
                    }
                }
                val container = Vector3f()
                val x = MVPMatrixRecorder.transform(transform.transformPosition(0.5f, 0f, 0f, container)).screenPosition
                val y = MVPMatrixRecorder.transform(transform.transformPosition(0f, 0.5f, 0f, container)).screenPosition
                val z = MVPMatrixRecorder.transform(transform.transformPosition(0f, 0f, 0.5f, container)).screenPosition

                addLine(center, x, guiGraphics, changeAlpha(X_COLOR, GLFW.GLFW_KEY_X))
                addLine(center, y, guiGraphics, changeAlpha(Y_COLOR, GLFW.GLFW_KEY_Y))
                addLine(center, z, guiGraphics, changeAlpha(Z_COLOR, GLFW.GLFW_KEY_Z))
            }
        }

        fun viewVectorDegreeCheckPass(partialTick: Float, sourcePosition: Vector3fc): Boolean {
            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            val viewVector = camera.forwardVector()
            val cameraPosition = camera.position()
            val sourceVector = Vector3f(
                (sourcePosition.x() - cameraPosition.x).toFloat(),
                (sourcePosition.y() - cameraPosition.y).toFloat(),
                (sourcePosition.z() - cameraPosition.z).toFloat()
            ).normalize()

            val dot = viewVector.dot(sourceVector)
            val angleInRadius = JomlMath.acos(dot)
            val angel = JomlMath.toDegrees(angleInRadius)
            return angel < Minecraft.getInstance().options.fov().get()
        }
    }
}
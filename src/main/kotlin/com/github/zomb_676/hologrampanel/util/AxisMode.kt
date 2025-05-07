package com.github.zomb_676.hologrampanel.util;

import com.github.zomb_676.hologrampanel.PanelOperatorManager
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.color.HologramColor
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderStateShard.LineStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import java.util.*

enum class AxisMode {
    WORLD {
        override fun extractX(player: Player, local: LocateType.World.FacingVector): Vector3f = Vector3f(1f, 0f, 0f)
        override fun extractY(player: Player, local: LocateType.World.FacingVector): Vector3f = Vector3f(0f, 1f, 0f)
        override fun extractZ(player: Player, local: LocateType.World.FacingVector): Vector3f = Vector3f(0f, 0f, 1f)

        override fun rotateX(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf =
            rotation.rotateLocalX(modifyDegree)

        override fun rotateY(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf =
            rotation.rotateLocalY(modifyDegree)

        override fun rotateZ(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf =
            rotation.rotateLocalZ(modifyDegree)
    },
    LOCAL {
        override fun extractX(player: Player, local: LocateType.World.FacingVector): Vector3f = local.getView().normalize(Vector3f())
        override fun extractY(player: Player, local: LocateType.World.FacingVector): Vector3f = local.getUp().normalize(Vector3f())
        override fun extractZ(player: Player, local: LocateType.World.FacingVector): Vector3f = local.getLeft().normalize(Vector3f())

        override fun rotateX(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf = rotation.rotateX(modifyDegree)
        override fun rotateY(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf = rotation.rotateY(modifyDegree)
        override fun rotateZ(rotation: Quaternionf, cameraRotation: Quaternionf, modifyDegree: Float): Quaternionf = rotation.rotateZ(modifyDegree)
    },
    PLAYER {
        private inline val camera get() = Minecraft.getInstance().gameRenderer.mainCamera
        override fun extractX(player: Player, local: LocateType.World.FacingVector): Vector3f = camera.lookVector.normalize(Vector3f())
        override fun extractY(player: Player, local: LocateType.World.FacingVector): Vector3f = camera.upVector.normalize(Vector3f())
        override fun extractZ(player: Player, local: LocateType.World.FacingVector): Vector3f = camera.leftVector.normalize(Vector3f())
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
    abstract fun extractX(player: Player, local: LocateType.World.FacingVector): Vector3f

    /**
     * the returned value can be modified safely
     */
    abstract fun extractY(player: Player, local: LocateType.World.FacingVector): Vector3f

    /**
     * the returned value can be modified safely
     */
    abstract fun extractZ(player: Player, local: LocateType.World.FacingVector): Vector3f

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
        private val LINES = RenderType.create(
            "lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                .setLineState(LineStateShard(OptionalDouble.of(10.0)))
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                .setCullState(RenderStateShard.NO_CULL)
                .createCompositeState(false)
        )

        fun drawAxisPrompt(event: RenderLevelStageEvent) {
            if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return
            val selectedTarget = PanelOperatorManager.selectedTarget ?: return
            val locate = selectedTarget.locate as? LocateType.World.FacingVector ?: return
            val pose = event.poseStack
            val partialTick = event.partialTick.getGameTimeDeltaPartialTick(false)
            val camPos = event.camera.position
//            pose.translate(-camPos.x, -camPos.y, -camPos.z)

            val worldPosition = selectedTarget.sourcePosition(partialTick)
            pose.translate(worldPosition.x(), worldPosition.y(), worldPosition.z())

            val mode = PanelOperatorManager.axisMode

            when (mode) {
                WORLD -> {}
                LOCAL -> {
                    pose.mulPose(locate.getMutableRotation())
                }

                PLAYER -> {
                    pose.mulPose(Minecraft.getInstance().gameRenderer.mainCamera.rotation())
                }
            }
            pose.last().pose().run {
                this.transformProject(0.5f, 0f, 0f, xVector)
                this.transformProject(0f, 0.5f, 0f, yVector)
                this.transformProject(0f, 0f, 0.5f, zVector)
            }
        }

        const val X_COLOR = 0xffff0000.toInt()
        const val Y_COLOR = 0xff00ff00.toInt()
        const val Z_COLOR = 0xff0000ff.toInt()

        val xVector = Vector3f()
        val yVector = Vector3f()
        val zVector = Vector3f()

        fun getCoordinateLayer(): LayeredDraw.Layer = object : LayeredDraw.Layer {
            private fun addLine(
                begin: ScreenPosition,
                end: ScreenPosition,
                consumer: VertexConsumer,
                matrix: Matrix4f,
                color: Int,
                width: Float = 2.0f
            ) {
                val vector = Vector2f(begin.y - end.y, end.x - begin.x).normalize().mul(width / 2)
                consumer.addVertex(matrix, begin.x + vector.x, begin.y + vector.y, 0f).setColor(color)
                consumer.addVertex(matrix, end.x + vector.x, end.y + vector.y, 0f).setColor(color)
                consumer.addVertex(matrix, end.x - vector.x, end.y - vector.y, 0f).setColor(color)
                consumer.addVertex(matrix, begin.x - vector.x, begin.y - vector.y, 0f).setColor(color)
            }

            private fun changeAlpha(originColor: Int, key: Int): Int {
                val window = Minecraft.getInstance().window.window
                return if (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
                    originColor
                } else {
                    HologramColor.argb(originColor).alpha(0x7f).color
                }
            }

            override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
                val target = PanelOperatorManager.selectedTarget ?: return
                val locate = target.locate as? LocateType.World.FacingVector ?: return
                val matrix = guiGraphics.pose().last().pose()

                val worldPosition = target.sourcePosition(deltaTracker.getGameTimeDeltaPartialTick(false))
                val center = MVPMatrixRecorder.transform(worldPosition).screenPosition
                val x = MVPMatrixRecorder.transform(xVector).screenPosition
                val y = MVPMatrixRecorder.transform(yVector).screenPosition
                val z = MVPMatrixRecorder.transform(zVector).screenPosition

                val buffer = guiGraphics.bufferSource.getBuffer(RenderType.gui())

                addLine(center, x, buffer, matrix, changeAlpha(X_COLOR, GLFW.GLFW_KEY_X))
                addLine(center, y, buffer, matrix, changeAlpha(Y_COLOR, GLFW.GLFW_KEY_Y))
                addLine(center, z, buffer, matrix, changeAlpha(Z_COLOR, GLFW.GLFW_KEY_Z))

                RenderSystem.disableCull()
                guiGraphics.bufferSource.endLastBatch()
                RenderSystem.enableCull()
            }
        }

    }
}
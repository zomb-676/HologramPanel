package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.payload.DebugStatisticsPayload
import com.github.zomb_676.hologrampanel.payload.QueryDebugStatisticsPayload
import com.github.zomb_676.hologrampanel.util.AutoTicker
import com.github.zomb_676.hologrampanel.util.FontBufferSource
import com.github.zomb_676.hologrampanel.util.InteractiveEntry
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.client.renderer.CoreShaders
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ARGB
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import org.joml.Vector3fc
import kotlin.math.max

object DebugHelper {
    private class DrawHelper(val guiGraphics: GuiGraphics, val padding: Int = 2) {
        private val font = Minecraft.getInstance().font
        private var x: Int = 0
        private var y: Int = 0
        private var xBegin = 0
        private var yStep = 0
        fun drawString(str: String): DrawHelper {
            guiGraphics.drawString(font, str, x, y, -1)
            x += font.width(str)
            yStep = max(yStep, font.lineHeight)
            return this
        }

        fun nextLine() {
            x = xBegin
            y += yStep + padding
            yStep = 0
        }
    }

    object Client {
        private val UPDATE_TINE: Int get() = max(Config.Server.updateInternal.get(), 20)
        private const val POPUP_TIME = 30
        private const val REMOVE_TIME = 30

        private const val UPDATE_BEGIN_COLOR : Int = 0xffffffff.toInt()
        private const val UPDATE_END_COLOR : Int = 0x00ffffff
        private const val POP_UP_BEGIN_COLOR : Int = 0xff0000ff.toInt()
        private const val POP_UP_END_COLOR : Int = 0x000000ff
        private const val REMOVE_BEGIN_COLOR : Int = 0xffff0000.toInt()
        private const val REMOVE_END_COLOR : Int = 0x00ff0000

        private const val OFFSET = 0.2

        private var lastDebugState: Boolean = false

        private val queryUpdateData: Object2IntOpenHashMap<UpdateEntry> = Object2IntOpenHashMap()

        data class UpdateEntry(val state: HologramRenderState, val size: Int)

        private val popUpData: Object2IntOpenHashMap<HologramRenderState> = Object2IntOpenHashMap()
        private val removeData: Object2IntOpenHashMap<HologramRenderState> = Object2IntOpenHashMap()

        private var interactRenderElement: IRenderElement? = null


        private fun tick(target: Object2IntOpenHashMap<*>) {
            val iterator = target.object2IntEntrySet().fastIterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val value = entry.intValue - 1
                if (value == 0) {
                    iterator.remove()
                } else {
                    entry.setValue(value)
                }
            }
        }

        fun tick(event: ClientTickEvent) {
            val current = Config.Client.renderDebugLayer.get()
            if (current != lastDebugState) {
                lastDebugState = current
                QueryDebugStatisticsPayload.query(current)
            }
            tick(queryUpdateData)
            tick(popUpData)
            tick(removeData)
        }

        fun fill(pos: Vector3fc, color: Int, poseStack: PoseStack, builder: BufferBuilder) {
            val r = ARGB.redFloat(color)
            val g = ARGB.greenFloat(color)
            val b = ARGB.blueFloat(color)
            val a = ARGB.alphaFloat(color)
            val x = pos.x()
            val y = pos.y()
            val z = pos.z()
            ShapeRenderer.addChainedFilledBoxVertices(
                poseStack, builder, x - OFFSET, y - OFFSET, z - OFFSET, x + OFFSET, y + OFFSET, z + OFFSET, r, g, b, a
            )
        }

        fun totalTickDataSize(): String {
            val iterator = this.queryUpdateData.object2IntEntrySet().iterator()
            var size = 0
            while (iterator.hasNext()) {
                val next = iterator.next()
                size += next.key.size
            }
            return byteSize(size)
        }

        private fun byteSize(size: Int) = when {
            size < (1 shl 10) -> "$size Bytes"
            size < (1 shl 20) -> "%.2f KiB".format(size.toFloat() / (1 shl 10))
            size < (1 shl 30) -> "%.2f MiB".format(size.toFloat() / (1 shl 20))
            else -> "%.2f GiB".format(size.toFloat() / (1 shl 20))
        }

        val fontBufferSource = FontBufferSource()

        fun renderLevelLast(event: RenderLevelStageEvent) {
            if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return
            val pose = event.poseStack
            val partialTick = event.partialTick.getGameTimeDeltaPartialTick(false)
            val camPos = event.camera.position
            pose.translate(-camPos.x, -camPos.y, -camPos.z)

            if (Config.Client.renderDebugHologramLifeCycleBox.get() && (queryUpdateData.isNotEmpty() || popUpData.isNotEmpty() || removeData.isNotEmpty())) {
                val builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)
                RenderSystem.setShader(CoreShaders.POSITION_COLOR)
                RenderSystem.disableDepthTest()
                if (queryUpdateData.isNotEmpty()) {
                    val iterator = queryUpdateData.object2IntEntrySet().fastIterator()
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        val color = ARGB.lerp((next.intValue + partialTick) / UPDATE_TINE, UPDATE_END_COLOR, UPDATE_BEGIN_COLOR)
                        val position = next.key.state.sourcePosition(partialTick)
                        fill(position, color, pose, builder)
                    }
                }
                if (popUpData.isNotEmpty()) {
                    val iterator = popUpData.object2IntEntrySet().fastIterator()
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        val color =
                            ARGB.lerp((next.intValue + partialTick) / POPUP_TIME, POP_UP_END_COLOR, POP_UP_BEGIN_COLOR)
                        fill(next.key.sourcePosition(partialTick), color, pose, builder)
                    }
                }
                if (removeData.isNotEmpty()) {
                    val iterator = removeData.object2IntEntrySet().fastIterator()
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        val color =
                            ARGB.lerp((next.intValue + partialTick) / REMOVE_TIME, REMOVE_END_COLOR, REMOVE_BEGIN_COLOR)
                        fill(next.key.sourcePosition(partialTick), color, pose, builder)
                    }
                }
                BufferUploader.drawWithShader(builder.buildOrThrow())
                RenderSystem.enableDepthTest()
            }
            if (Config.Client.renderNetworkDebugInfo.get() && queryUpdateData.isNotEmpty()) {
                val iterator = queryUpdateData.object2IntEntrySet().fastIterator()
                val font = Minecraft.getInstance().font
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    val position = next.key.state.sourcePosition(partialTick)
                    val size = next.key.size
                    pose.stack {
                        pose.translate(position.x(), position.y() + 0.6f, position.z())
                        pose.mulPose(Minecraft.getInstance().entityRenderDispatcher.cameraOrientation())
                        pose.scale(0.0125f, -0.0125f, 0.0125f)
                        val text = byteSize(size)
                        font.drawInBatch(
                            text, -(font.width(text) / 2).toFloat(), 0f, -2130706433, false, pose.last().pose(),
                            fontBufferSource, Font.DisplayMode.SEE_THROUGH, 1056964608, LightTexture.FULL_BRIGHT
                        )
                    }
                }
                fontBufferSource.endFontBatch()
            }
        }

        fun getLayer() = object : LayeredDraw.Layer {
            override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
                if (Minecraft.getInstance().gui.debugOverlay.showDebugScreen()) return
                if (!Config.Client.renderDebugLayer.get()) return
                val drawHelper = DrawHelper(guiGraphics)
                drawHelper.drawString("syncRate:${Config.Server.updateInternal.get()}Tick").nextLine()
                if (Config.Client.renderNetworkDebugInfo.get()) {
                    drawHelper.drawString("synced data size:${totalTickDataSize()}, count:${queryUpdateData.size}").nextLine()
                } else {
                    drawHelper.drawString("synced data size: require enable renderNetworkDebugInfo").nextLine()
                }
                drawHelper.drawString("current widget count : ${HologramManager.widgetCount()}").nextLine()
                drawHelper.drawString(querySyncString()).nextLine()
                drawHelper.drawString("displayed:${HologramManager.states.values.count { it.displayed }}").nextLine()
                drawHelper.drawString("collapseTarget:${HologramManager.getCollapseTarget()}").nextLine()
                drawHelper.drawString("interactRenderElement:${interactRenderElement}").nextLine()
                val enable = Config.Server.allowHologramInteractive.get()
                drawHelper.drawString("interactiveTarget:${HologramManager.getInteractiveTarget()}, enable:$enable").nextLine()
                when (val interactHologram = HologramManager.getInteractHologram()) {
                    is HologramRenderState -> {
                        drawHelper.drawString("InteractHologramContext:${interactHologram.context}").nextLine()
                        val tickets = interactHologram.hologramTicks.joinToString()
                        drawHelper.drawString("Ticket:$tickets").nextLine()
                    }
                }
                when (val draggingSource = HologramInteractionManager.draggingSource) {
                    is InteractiveEntry -> {
                        drawHelper.drawString("draggingSource:$draggingSource").nextLine()
                    }
                }
                when (val dragContext = HologramInteractionManager.dragData) {
                    is HologramInteractionManager.DragDataContext<*> -> {
                        drawHelper.drawString(
                            "dragContext:(data:${dragContext.getDragData()},valid:${dragContext.isStillValid()}," +
                                    "type:${dragContext.getDragDataClass()?.simpleName})"
                        ).nextLine()
                    }
                }
            }
        }

        fun onJoinLevel() {
            val configValue = Config.Client.renderDebugLayer.get()
            if (configValue) {
                QueryDebugStatisticsPayload.query(true)
            }
            lastDebugState = configValue
        }

        private fun querySyncString(): String {
            val builder = StringBuilder("sync:")
            builder.append("client:${DataQueryManager.Client.syncCount()},")
            builder.append("server:(${DebugStatisticsPayload.SYNC_COUNT_FOR_PLAYER}/${DebugStatisticsPayload.TOTAL_SYNC_COUNT})")
            return builder.toString()
        }

        fun onDataReceived(widget: DynamicBuildWidget<*>, sizeInBytes: Int) {
            if (Config.Client.renderDebugHologramLifeCycleBox.get() || Config.Client.renderNetworkDebugInfo.get()) {
                val state = HologramManager.queryHologramState(widget) ?: return
                queryUpdateData.put(UpdateEntry(state, sizeInBytes), UPDATE_TINE)
            }
        }

        fun recordPopup(state: HologramRenderState) {
            if (!Config.Client.renderDebugHologramLifeCycleBox.get()) return
            popUpData.put(state, POPUP_TIME)
        }

        fun recordRemove(state: HologramRenderState) {
            if (!Config.Client.renderDebugHologramLifeCycleBox.get()) return
            removeData.put(state, REMOVE_TIME)
        }

        fun recordHoverElement(element: IRenderElement) {
            this.interactRenderElement = element
        }

        fun clearRenderRelatedInfo() {
            this.interactRenderElement = null
        }
    }

    object Server {
        val queryPlayer: MutableSet<ServerPlayer> = mutableSetOf()
        val updateTick = AutoTicker.by(5)

        fun onPlayerLogout(player: ServerPlayer) {
            queryPlayer.remove(player)
        }

        fun onPacket(payload: QueryDebugStatisticsPayload, context: IPayloadContext) {
            val player = context.player() as ServerPlayer
            if (payload.enable) {
                queryPlayer.add(player)
            } else {
                queryPlayer.remove(player)
            }
        }

        fun serverTick() {
            updateTick.tryConsume {
                val totalSyncCount = DataQueryManager.Server.syncCount()
                queryPlayer.forEach { player ->
                    val forPlayer = DataQueryManager.Server.syncCountForPlayer(player)
                    val payload = DebugStatisticsPayload(totalSyncCount, forPlayer)
                    player.connection.send(payload)
                }
            }
        }
    }
}
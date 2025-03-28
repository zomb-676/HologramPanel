package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand
import com.github.zomb_676.hologrampanel.interaction.InteractionModeManager
import com.github.zomb_676.hologrampanel.payload.DebugStatisticsPayload
import com.github.zomb_676.hologrampanel.payload.QueryDebugStatisticsPayload
import com.github.zomb_676.hologrampanel.util.AutoTicker
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.github.zomb_676.hologrampanel.widget.dynamic.IRenderElement
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.client.renderer.CoreShaders
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ARGB
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import org.joml.Vector3fc
import kotlin.math.max

object DebugHelper {
    object Client {
        const val COMMAND_LASTING_TIME = 3
        private val UPDATE_TINE: Int get() = max(Config.Server.updateInternal.get(), 20)
        private const val POPUP_TIME = 30
        private const val REMOVE_TIME = 30

        private const val OFFSET = 0.2

        private var lastDebugState: Boolean = false

        private var command: InteractionCommand.Exact? = null
        private var remainTimeInTicks: Int = 0
        private val queryUpdateData: Object2IntOpenHashMap<HologramRenderState> = Object2IntOpenHashMap()
        private val popUpData: Object2IntOpenHashMap<HologramRenderState> = Object2IntOpenHashMap()
        private val removeData: Object2IntOpenHashMap<HologramRenderState> = Object2IntOpenHashMap()

        private var lookingRenderElement: IRenderElement? = null

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
                if (!current) {
                    queryUpdateData.clear()
                    popUpData.clear()
                    removeData.clear()
                }
            }
            if (!current) return
            if (remainTimeInTicks > 0) {
                --remainTimeInTicks
            }
            tick(queryUpdateData)
            tick(popUpData)
            tick(removeData)
        }

        fun updateExactCommand(command: InteractionCommand.Exact?) {
            if (command != null) {
                this.command = command
                this.remainTimeInTicks = COMMAND_LASTING_TIME
            }
        }

        fun renderCommand(x: Int, y: Int, guiGraphics: GuiGraphics, partialTick: Float) {
            if (remainTimeInTicks > 0 && command != null) {
                val color =
                    ARGB.lerp((this.remainTimeInTicks + partialTick) / COMMAND_LASTING_TIME, 0x00ffffff.toInt(), -1)
                guiGraphics.drawString(Minecraft.getInstance().font, command.toString(), x, y, color)
            }
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

        fun renderLevelLast(event: RenderLevelStageEvent) {
            if (!Config.Client.renderDebugLayer.get()) return
            if (!Config.Client.renderDebugBox.get()) return
            if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return
            if (queryUpdateData.isEmpty() && popUpData.isEmpty() && removeData.isEmpty()) return

            val pose = event.poseStack
            val builder =
                Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)
            RenderSystem.setShader(CoreShaders.POSITION_COLOR)
            RenderSystem.disableDepthTest()

            val camPos = event.camera.position
            pose.translate(-camPos.x, -camPos.y, -camPos.z)
            val partialTick = event.partialTick.getGameTimeDeltaPartialTick(true)
            if (queryUpdateData.isNotEmpty()) {
                val iterator = queryUpdateData.object2IntEntrySet().fastIterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    val color = ARGB.lerp((next.intValue + partialTick) / UPDATE_TINE, 0x00ffffff.toInt(), -1)
                    fill(next.key.sourcePosition(partialTick), color, pose, builder)
                }
            }
            if (popUpData.isNotEmpty()) {
                val iterator = popUpData.object2IntEntrySet().fastIterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    val color =
                        ARGB.lerp((next.intValue + partialTick) / POPUP_TIME, 0x000000ff.toInt(), 0xff0000ff.toInt())
                    fill(next.key.sourcePosition(partialTick), color, pose, builder)
                }
            }
            if (removeData.isNotEmpty()) {
                val iterator = removeData.object2IntEntrySet().fastIterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    val color =
                        ARGB.lerp((next.intValue + partialTick) / REMOVE_TIME, 0x00ff0000.toInt(), 0xffff0000.toInt())
                    fill(next.key.sourcePosition(partialTick), color, pose, builder)
                }
            }
            BufferUploader.drawWithShader(builder.buildOrThrow())
            RenderSystem.enableDepthTest()
        }

        fun getLayer() = object : LayeredDraw.Layer {
            override fun render(
                guiGraphics: GuiGraphics, deltaTracker: DeltaTracker
            ) {
                if (Minecraft.getInstance().gui.debugOverlay.showDebugScreen()) return
                if (!Config.Client.renderDebugLayer.get()) return
                val font = Minecraft.getInstance().font
                renderCommand(10, 5, guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))
                guiGraphics.drawString(font, InteractionModeManager.mode.toString(), 10, 10, -1)
                guiGraphics.drawString(font, "syncRate:${Config.Server.updateInternal.get()}Tick", 10, 20, -1)
                guiGraphics.drawString(font, "current widget count : ${HologramManager.widgetCount()}", 10, 30, -1)
                guiGraphics.drawString(font, querySyncString(), 10, 40, -1)
                guiGraphics.drawString(
                    font, "displayed:${HologramManager.states.values.count { it.displayed }}", 10, 50, -1
                )
                guiGraphics.drawString(font, "lookingRenderElement:${lookingRenderElement}", 10, 60, -1)
                guiGraphics.drawString(font, "interactiveTarget:${HologramManager.getInteractiveTarget()}", 10, 70, -1)
                val lookingHologram = HologramManager.getLookingHologram() ?: return
                guiGraphics.drawString(font, "lookingHologramContext:${lookingHologram.context}", 10, 80, -1)
                val tickets = lookingHologram.hologramTicks.joinToString()
                guiGraphics.drawString(font, "lookTicket:$tickets", 10, 90, -1)
            }
        }

        fun onJoinLevel() {
            val configValue = Config.Client.renderDebugLayer.get()
            if (configValue) {
                QueryDebugStatisticsPayload.query(true)
            }
            lastDebugState = configValue
        }

        fun querySyncString(): String {
            val builder = StringBuilder("sync:")
            builder.append("client:${DataQueryManager.Client.syncCount()},")
            builder.append("server:(${DebugStatisticsPayload.SYNC_COUNT_FOR_PLAYER}/${DebugStatisticsPayload.TOTAL_SYNC_COUNT})")
            return builder.toString()
        }

        fun onDataReceived(widget: DynamicBuildWidget<*>) {
            if (!Config.Client.renderDebugLayer.get()) return
            val state = HologramManager.queryHologramState(widget) ?: return
            queryUpdateData.put(state, UPDATE_TINE)
        }

        fun recordPopup(state: HologramRenderState) {
            if (!Config.Client.renderDebugLayer.get()) return
            popUpData.put(state, POPUP_TIME)
        }

        fun recordRemove(state: HologramRenderState) {
            if (!Config.Client.renderDebugLayer.get()) return
            removeData.put(state, REMOVE_TIME)
        }

        fun recordHoverElement(element: IRenderElement) {
            this.lookingRenderElement = element
        }

        fun clearRenderRelatedInfo() {
            this.lookingRenderElement = null
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
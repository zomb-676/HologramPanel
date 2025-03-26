package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand
import com.github.zomb_676.hologrampanel.interaction.InteractionModeManager
import com.github.zomb_676.hologrampanel.render.RenderStuff
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.mojang.blaze3d.vertex.*
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.util.ARGB
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks
import org.joml.Vector3fc

object DebugHelper {
    const val COMMAND_LASTING_TIME = 3
    private val UPDATE_TINE: Int get() = Config.Server.updateInternal.get()
    private const val POPUP_TIME = 30
    private const val REMOVE_TIME = 30

    private const val OFFSET = 0.2

    private var command: InteractionCommand.Exact? = null
    private var remainTimeInTicks: Int = 0
    private val queryUpdateData: Object2IntOpenHashMap<HologramRenderState> = Object2IntOpenHashMap()
    private val popUpData: Object2IntOpenHashMap<HologramRenderState> = Object2IntOpenHashMap()
    private val removeData: Object2IntOpenHashMap<HologramRenderState> = Object2IntOpenHashMap()

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
            val color = ARGB.lerp((this.remainTimeInTicks + partialTick) / COMMAND_LASTING_TIME, 0x00ffffff.toInt(), -1)
            guiGraphics.drawString(Minecraft.getInstance().font, command.toString(), x, y, color)
        }
    }

    fun fill(pos: Vector3fc, color: Int, poseStack: PoseStack, builder: VertexConsumer) {
        val r = ARGB.redFloat(color)
        val g = ARGB.greenFloat(color)
        val b = ARGB.blueFloat(color)
        val a = ARGB.alphaFloat(color)
        val x = pos.x()
        val y = pos.y()
        val z = pos.z()
        ShapeRenderer.addChainedFilledBoxVertices(
            poseStack,
            builder,
            x - OFFSET,
            y - OFFSET,
            z - OFFSET,
            x + OFFSET,
            y + OFFSET,
            z + OFFSET,
            r,
            g,
            b,
            a
        )
    }

    fun renderLevelLast(event: RenderLevelStageEvent) {
        if (!Config.Client.renderDebugLayer.get()) return
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return
        if (queryUpdateData.isEmpty()) return

        val pose = event.poseStack
        val bufferSource = Minecraft.getInstance().renderBuffers().bufferSource()
        val builder = bufferSource.getBuffer(RenderStuff.Type.DEBUG_FILLED_BOX_DISABLE_DEPTH)

        val camPos = event.camera.position
        pose.translate(-camPos.x, -camPos.y, -camPos.z)
        val partialTick = event.partialTick.getGameTimeDeltaPartialTick(true)
        run {
            val iterator = queryUpdateData.object2IntEntrySet().fastIterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                val color = ARGB.lerp((next.intValue + partialTick) / UPDATE_TINE, 0x00ffffff.toInt(), -1)
                fill(next.key.sourcePosition(partialTick), color, pose, builder)
            }
        }
        run {
            val iterator = popUpData.object2IntEntrySet().fastIterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                val color =
                    ARGB.lerp((next.intValue + partialTick) / POPUP_TIME, 0x000000ff.toInt(), 0xff0000ff.toInt())
                fill(next.key.sourcePosition(partialTick), color, pose, builder)
            }
        }
        run {
            val iterator = removeData.object2IntEntrySet().fastIterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                val color =
                    ARGB.lerp((next.intValue + partialTick) / REMOVE_TIME, 0x00ff0000.toInt(), 0xffff0000.toInt())
                fill(next.key.sourcePosition(partialTick), color, pose, builder)
            }
        }
        bufferSource.endLastBatch()
    }

    fun getLayer() = object : LayeredDraw.Layer {
        override fun render(
            guiGraphics: GuiGraphics,
            deltaTracker: DeltaTracker
        ) {
            if (Minecraft.getInstance().gui.debugOverlay.showDebugScreen()) return
            if (!Config.Client.renderDebugLayer.get()) return
            val font = Minecraft.getInstance().font
            renderCommand(10, 10, guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))
            guiGraphics.drawString(font, InteractionModeManager.mode.toString(), 10, 20, -1)
            guiGraphics.drawString(font, "current widget count : ${HologramManager.widgetCount()}", 10, 30, -1)
            val syncCount = if (ServerLifecycleHooks.getCurrentServer() == null) {
                "client:${DataQueryManager.Client.syncCount()}"
            } else {
                "client:${DataQueryManager.Client.syncCount()},server:${DataQueryManager.Server.syncCount()}"
            }
            guiGraphics.drawString(font, syncCount, 10, 40, -1)
            guiGraphics.drawString(
                font,
                "displayed:${HologramManager.states.values.count { it.displayed }}",
                10,
                50,
                -1
            )
        }
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
}
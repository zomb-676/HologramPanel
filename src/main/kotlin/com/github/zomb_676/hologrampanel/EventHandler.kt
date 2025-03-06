package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.CycleSelector
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand
import com.github.zomb_676.hologrampanel.interaction.InteractionModeManager
import com.github.zomb_676.hologrampanel.payload.*
import com.github.zomb_676.hologrampanel.sync.DataSynchronizer
import com.github.zomb_676.hologrampanel.sync.SynchronizerManager
import com.github.zomb_676.hologrampanel.util.CommandDSL
import com.github.zomb_676.hologrampanel.widget.InteractionLayer
import com.github.zomb_676.hologrampanel.widget.interactive.HologramInteractiveHelper
import com.github.zomb_676.hologrampanel.widget.interactive.HologramInteractiveTarget
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.commands.arguments.coordinates.WorldCoordinates
import net.minecraft.server.level.ServerPlayer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.*
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import org.lwjgl.glfw.GLFW

object EventHandler {
    fun initEvents(dist: Dist, modBus: IEventBus) {

        val forgeBus = NeoForge.EVENT_BUS
        modBus.addListener(::registerPayload)
        forgeBus.addListener(::registerCommand)
        forgeBus.addListener(::tickClientPostEvent)
        forgeBus.addListener(::tickServerPostEvent)
        forgeBus.addListener(::onPlayerChangeDimension)
        forgeBus.addListener(::onPlayerLogin)
        forgeBus.addListener(::onPlayerLogout)
        forgeBus.addListener(::levelUnload)
        if (dist == Dist.CLIENT) {
            ClientOnly.initEvents(modBus)
        }
    }

    object ClientOnly {
        fun initEvents(modBus: IEventBus) {
            val forgeBus = NeoForge.EVENT_BUS
            modBus.addListener(::registerLayer)
            modBus.addListener(::registerKey)
            forgeBus.addListener(::onKey)
            forgeBus.addListener(::onMouseButton)
            forgeBus.addListener(::onMouseScroll)
            forgeBus.addListener(::onInteraction)
            forgeBus.addListener(::onClientTickPre)
            forgeBus.addListener(::onClientTickPost)
            forgeBus.addListener(::onRenderGUI)
            forgeBus.addListener(::onPlayerLogout)
        }

        val switchModeKey by lazy {
            KeyMapping(
                "key.a.switch_mode_key",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "key.categories.misc"
            )
        }

        private fun registerKey(event: RegisterKeyMappingsEvent) {
            event.register(switchModeKey)
        }

        private fun onClientTickPost(event: ClientTickEvent.Post) {
            while (switchModeKey.consumeClick()) {
                InteractionModeManager.switchModeKeyToggled()
            }
        }

        private fun onClientTickPre(event: ClientTickEvent.Pre) {
            InteractionLayer.tick(event)
        }

        private fun onRenderGUI(event: RenderGuiEvent.Post) {
            val font = Minecraft.getInstance().font
            InteractionLayer.renderCommand(
                10, 10, event.guiGraphics, event.partialTick.getGameTimeDeltaPartialTick(false)
            )
            event.guiGraphics.drawString(font, InteractionModeManager.mode.toString(), 10, 20, -1)
        }

        fun onWindowResize(width: Int, height: Int) {

        }

        private fun onKey(event: InputEvent.Key) {
            if (Minecraft.getInstance().level == null) return
            if (Minecraft.getInstance().screen != null) return
            if (InteractionModeManager.mode.isDisable()) return
            InteractionCommand.Raw.Key.create(event).post()
        }

        private fun onMouseScroll(event: InputEvent.MouseScrollingEvent) {
            if (Minecraft.getInstance().level == null) return
            if (Minecraft.getInstance().screen != null) return
            if (InteractionModeManager.mode.isDisable()) return
            event.isCanceled = true
            InteractionCommand.Raw.MouseScroll.create(event).post()
        }

        private fun onMouseButton(event: InputEvent.MouseButton.Pre) {
            if (Minecraft.getInstance().level == null) return
            if (Minecraft.getInstance().screen != null) return
            if (InteractionModeManager.mode.isDisable()) return
            if (InteractionModeManager.shouldRestPlayerClientInput() && event.action != GLFW.GLFW_RELEASE) {
                event.isCanceled = true
            }
            InteractionCommand.Raw.MouseButton.create(event).post()
        }

        private fun onInteraction(event: InputEvent.InteractionKeyMappingTriggered) {
            if (Minecraft.getInstance().level == null) return
            if (Minecraft.getInstance().screen != null) return
            if (InteractionModeManager.mode.isDisable()) return
            event.isCanceled = true
        }

        private fun registerLayer(event: RegisterGuiLayersEvent) {
            event.registerAboveAll(HologramPanel.rl("interaction_mode_layer"), InteractionLayer.getLayer())
            event.registerAboveAll(HologramPanel.rl("cycle_selector")) { guiGraphics, deltaTracker ->
                CycleSelector.currentInstance()?.render(guiGraphics, deltaTracker)
            }
        }

        private fun onPlayerLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
            HologramPanel.serverInstalled = false
        }
    }

    private fun registerPayload(event: RegisterPayloadHandlersEvent) {
        event.registrar("1.0").playToClient<HologramCreatePayload<*>>(
            HologramCreatePayload.TYPE, HologramCreatePayload.STREAM_CODEC, HologramCreatePayload.HANDLE
        ).playBidirectional<DataSynchronizerSyncPayload>(
            DataSynchronizerSyncPayload.TYPE,
            DataSynchronizerSyncPayload.STREAM_CODEC,
            DataSynchronizerSyncPayload.HANDLE
        ).playToClient<ServerHandShakePayload>(
            ServerHandShakePayload.TYPE, ServerHandShakePayload.STREAM_CODEC, ServerHandShakePayload.HANDLE
        )
    }

    private fun registerCommand(event: RegisterCommandsEvent) {
        CommandDSL(event.dispatcher).apply {
            HologramPanel.MOD_ID {
                "test_open" {
                    "pos"(BlockPosArgument.blockPos()) {
                        execute {
                            val player = this.source.player!!
                            val pos = this.getArgument("pos", WorldCoordinates::class.java)

                            HologramInteractiveHelper.openOnServer(
                                player, HologramInteractiveTarget.Companion.Furnace
                            ) {
                                it.writeBlockPos(pos.getBlockPos(this.source))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as ServerPlayer
        val payload = ServerHandShakePayload()
        player.connection.send(payload)
    }

    private fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity
        require(!player.level().isClientSide)

        SynchronizerManager.Server.clearForPlayer(player as ServerPlayer)
    }

    private fun onPlayerChangeDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        val player = event.entity
        require(!player.level().isClientSide)

        SynchronizerManager.Server.clearForPlayer(player as ServerPlayer)
    }

    private fun tickServerPostEvent(event: ServerTickEvent.Post) {
        SynchronizerManager.Server.syncers.values.forEach(DataSynchronizer::tick)
    }

    private fun tickClientPostEvent(event: ClientTickEvent.Post) {
        SynchronizerManager.Client.syncers.values.forEach(DataSynchronizer::tick)
    }

    private fun levelUnload(event: LevelEvent.Unload) {
        if (event.level.isClientSide) {
            HologramManager.clearHologram()
            InteractionModeManager.clearState()
            SynchronizerManager.Client.syncers.clear()
        }
    }
}
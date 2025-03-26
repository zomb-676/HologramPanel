package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand
import com.github.zomb_676.hologrampanel.interaction.InteractionModeManager
import com.github.zomb_676.hologrampanel.payload.*
import com.github.zomb_676.hologrampanel.util.CommandDSL
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector
import com.github.zomb_676.hologrampanel.widget.InteractionLayer
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.brigadier.arguments.BoolArgumentType
import net.minecraft.client.DeltaTracker
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.server.level.ServerPlayer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent
import net.neoforged.neoforge.client.event.*
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.registries.RegisterEvent
import org.lwjgl.glfw.GLFW

object EventHandler {
    fun initEvents(dist: Dist, modBus: IEventBus) {

        val forgeBus = NeoForge.EVENT_BUS
        modBus.addListener(EventHandler::registerPayload)
        forgeBus.addListener(EventHandler::registerCommand)
        forgeBus.addListener(EventHandler::registerClientCommand)
        forgeBus.addListener(EventHandler::tickClientPostEvent)
        forgeBus.addListener(EventHandler::tickServerPostEvent)
        forgeBus.addListener(EventHandler::onPlayerChangeDimension)
        forgeBus.addListener(EventHandler::onPlayerLogin)
        forgeBus.addListener(EventHandler::onPlayerLogout)
        forgeBus.addListener(EventHandler::levelUnload)
        modBus.addListener(EventHandler::onRegistryEvent)
        modBus.addListener(EventHandler::onClientSetup)
        modBus.addListener(EventHandler::onLoadComplete)
        if (dist == Dist.CLIENT) {
            ClientOnly.initEvents(modBus)
            forgeBus.addListener(::onEntityJoinLevel)
        }
    }

    object ClientOnly {
        fun initEvents(modBus: IEventBus) {
            val forgeBus = NeoForge.EVENT_BUS
            modBus.addListener(ClientOnly::registerLayer)
            modBus.addListener(ClientOnly::registerKey)
            forgeBus.addListener(ClientOnly::onKey)
            forgeBus.addListener(ClientOnly::onMouseButton)
            forgeBus.addListener(ClientOnly::onMouseScroll)
            forgeBus.addListener(ClientOnly::onInteraction)
            forgeBus.addListener(ClientOnly::onClientTickPre)
            forgeBus.addListener(ClientOnly::onClientTickPost)
            forgeBus.addListener(ClientOnly::onRenderGUI)
            forgeBus.addListener(ClientOnly::onPlayerLogin)
            forgeBus.addListener(ClientOnly::onPlayerLogout)
            forgeBus.addListener(ClientOnly::onRenderLevelStage)
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

        val panelKey by lazy {
            KeyMapping(
                "key.a.selector_panel",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "key.categories.misc"
            )
        }

        private fun registerKey(event: RegisterKeyMappingsEvent) {
            event.register(switchModeKey)
            event.register(panelKey)
        }

        private fun onClientTickPost(event: ClientTickEvent.Post) {
            while (switchModeKey.consumeClick()) {
                InteractionModeManager.switchModeKeyToggled()
            }
            if (panelKey.isDown) {
                CycleSelector.tryBegin()
            } else {
                CycleSelector.tryEnd()
            }
        }

        private fun onPlayerLogin(event: ClientPlayerNetworkEvent.LoggingIn) {
            DebugHelper.Client.onJoinLevel()
        }

        private fun onClientTickPre(event: ClientTickEvent.Pre) {
            DebugHelper.Client.tick(event)
        }

        private fun onRenderGUI(event: RenderGuiEvent.Post) {
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
            if (CycleSelector.preventPlayerTurn()) {
                event.isCanceled = true
                if (event.action == GLFW.GLFW_PRESS) {
                    CycleSelector.onClick()
                }
                return
            }

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
            event.registerBelow(
                VanillaGuiLayers.CROSSHAIR,
                HologramPanel.rl("interaction_mode_layer"),
                InteractionLayer.getLayer()
            )
            event.registerAboveAll(HologramPanel.rl("cycle_selector"), object : LayeredDraw.Layer {
                override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
                    CycleSelector.render(guiGraphics, deltaTracker)
                }
            })
            event.registerAboveAll(HologramPanel.rl("debug_layer"), DebugHelper.Client.getLayer())
        }

        private fun onPlayerLogIn(event: ClientPlayerNetworkEvent.LoggingIn) {

        }


        private fun onPlayerLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
            HologramPanel.serverInstalled = false
        }

        fun onRenderLevelStage(event: RenderLevelStageEvent) {
            DebugHelper.Client.renderLevelLast(event)
        }
    }

    private fun registerPayload(event: RegisterPayloadHandlersEvent) {
        event.registrar("1.0").playToClient<ServerHandShakePayload>(
            ServerHandShakePayload.TYPE, ServerHandShakePayload.STREAM_CODEC, ServerHandShakePayload.HANDLE
        ).playToServer<ComponentRequestDataPayload<*>>(
            ComponentRequestDataPayload.TYPE,
            ComponentRequestDataPayload.STREAM_CODEC,
            ComponentRequestDataPayload.HANDLE
        ).playToClient<ComponentResponseDataPayload>(
            ComponentResponseDataPayload.TYPE,
            ComponentResponseDataPayload.STREAM_CODEC,
            ComponentResponseDataPayload.HANDLE
        ).playBidirectional<SyncClosePayload>(
            SyncClosePayload.TYPE,
            SyncClosePayload.STREAM_CODEC,
            SyncClosePayload.HANDLE
        ).playToClient<EntityConversationPayload>(
            EntityConversationPayload.TYPE,
            EntityConversationPayload.STREAM_CODEC,
            EntityConversationPayload.HANDLE
        ).playToServer<QueryDebugStatisticsPayload>(
            QueryDebugStatisticsPayload.TYPE,
            QueryDebugStatisticsPayload.STREAM_CODEC,
            QueryDebugStatisticsPayload.HANDLE
        ).playToClient<DebugStatisticsPayload>(
            DebugStatisticsPayload.TYPE,
            DebugStatisticsPayload.STREAM_CODEC,
            DebugStatisticsPayload.HANDLE
        )
    }

    private fun registerCommand(event: RegisterCommandsEvent) {

    }

    private fun registerClientCommand(event : RegisterClientCommandsEvent) {
        CommandDSL(event.dispatcher).apply {
            HologramPanel.MOD_ID {
                "debug_layer" {
                    "debug_layer"(BoolArgumentType.bool()) {
                        execute {
                            val value = BoolArgumentType.getBool(this, "debug_layer")
                            Config.Client.renderDebugLayer.set(value)
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
        val player = event.entity as ServerPlayer

        DataQueryManager.Server.clearForPlayer(player)
        DebugHelper.Server.onPlayerLogout(player)
    }

    /**
     * always on logic server
     */
    private fun onPlayerChangeDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        val player = event.entity as ServerPlayer
        require(!player.level().isClientSide)

        DataQueryManager.Server.clearForPlayer(player)
    }

    private fun tickServerPostEvent(event: ServerTickEvent.Post) {
        DataQueryManager.Server.tick()
        DebugHelper.Server.serverTick()
    }

    private fun tickClientPostEvent(event: ClientTickEvent.Post) {
        HologramManager.clientTick()
        if (Config.Client.enablePopUp.get()) {
            PopupManager.tickPopup()
        }
    }

    private fun levelUnload(event: LevelEvent.Unload) {
        if (event.level.isClientSide) {
            HologramManager.clearHologram()
            InteractionModeManager.clearState()
            DataQueryManager.Client.closeAll()
            EntityConversationPayload.clear()
        }
    }

    private fun onRegistryEvent(event: RegisterEvent) {
        if (event.registryKey == AllRegisters.ComponentHologramProviderRegistry.RESOURCE_KEY) {
            PluginManager.getInstance().commonRegistration.forEach { (plugin, reg) ->
                plugin.registerCommon(reg)
                event.register(AllRegisters.ComponentHologramProviderRegistry.RESOURCE_KEY) { helper ->
                    reg.blockProviders.forEach { provider ->
                        helper.register(provider.location(), provider)
                    }
                    reg.entityProviders.forEach { provider ->
                        helper.register(provider.location(), provider)
                    }
                }
            }
        }
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        PluginManager.getInstance().clientRegistration.forEach { (plugin, reg) ->
            plugin.registerClient(reg)
        }
    }

    private fun onLoadComplete(event: FMLLoadCompleteEvent) {

    }

    /**
     * we use this event to know that all registries have frozen
     */
    fun onRegistryEnd(
//        event: IdMappingEvent
    ) {
        PluginManager.onLoadComplete()
        PluginManager.getInstance().onClientRegisterEnd()
    }

    private fun onEntityJoinLevel(event: EntityJoinLevelEvent) {
        val level = event.level
        if (level.isClientSide) {
            EntityConversationPayload.onEntityJoin(event.entity)
        }
    }
}
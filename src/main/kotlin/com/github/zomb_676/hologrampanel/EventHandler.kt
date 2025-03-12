package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.CycleSelector
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand
import com.github.zomb_676.hologrampanel.interaction.InteractionModeManager
import com.github.zomb_676.hologrampanel.payload.ComponentRequestDataPayload
import com.github.zomb_676.hologrampanel.payload.ComponentResponseDataPayload
import com.github.zomb_676.hologrampanel.payload.ServerHandShakePayload
import com.github.zomb_676.hologrampanel.payload.SyncClosePayload
import com.github.zomb_676.hologrampanel.util.CommandDSL
import com.github.zomb_676.hologrampanel.widget.InteractionLayer
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
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
import net.neoforged.neoforge.registries.RegisterEvent
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
        modBus.addListener(::onRegistryEvent)
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
        ).playToServer<SyncClosePayload>(
            SyncClosePayload.TYPE,
            SyncClosePayload.STREAM_CODEC,
            SyncClosePayload.HANDLE
        )
    }

    private fun registerCommand(event: RegisterCommandsEvent) {
        CommandDSL(event.dispatcher).apply {
            HologramPanel.MOD_ID {

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
    }

    private fun tickClientPostEvent(event: ClientTickEvent.Post) {
    }

    private fun levelUnload(event: LevelEvent.Unload) {
        if (event.level.isClientSide) {
            HologramManager.clearHologram()
            InteractionModeManager.clearState()
            DataQueryManager.Client.closeAll()
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
}
package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.interaction.HologramInteractionManager
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.payload.*
import com.github.zomb_676.hologrampanel.render.TransitRenderTargetManager
import com.github.zomb_676.hologrampanel.util.*
import com.github.zomb_676.hologrampanel.util.selector.CycleSelector
import com.github.zomb_676.hologrampanel.widget.InteractionLayer
import com.github.zomb_676.hologrampanel.widget.LocateType
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerPlayer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent
import net.neoforged.neoforge.client.event.*
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks
import net.neoforged.neoforge.server.command.EnumArgument
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.min

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
        forgeBus.addListener(EventHandler::onMobConversion)
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

        private fun registerKey(event: RegisterKeyMappingsEvent) {
            AllRegisters.KeyMapping.register(event)
        }

        private fun onClientTickPost(event: ClientTickEvent.Post) {

        }

        private fun onPlayerLogin(event: ClientPlayerNetworkEvent.LoggingIn) {
            DebugHelper.Client.onJoinLevel()
        }

        private fun onClientTickPre(event: ClientTickEvent.Pre) {
            DebugHelper.Client.tick(event)
        }

        private fun onRenderGUI(event: RenderGuiEvent.Pre) {
            MousePositionManager.updateMousePosition()
        }

        fun onWindowResize(width: Int, height: Int) {
            TransitRenderTargetManager.onResize(width, height)
        }

        private fun onKey(event: InputEvent.Key) {
            if (Minecraft.getInstance().level == null) return
            if (Minecraft.getInstance().screen != null) return

            val isDownAction = event.action == GLFW.GLFW_PRESS
            val isRelease = event.action == GLFW.GLFW_RELEASE
            when (event.key) {
                AllRegisters.KeyMapping.panelKey.key.value -> if (isDownAction) {
                    CycleSelector.tryBegin()
                } else if (isRelease) {
                    CycleSelector.tryEnd()
                }

                AllRegisters.KeyMapping.freeMouseMoveKey.key.value -> {
                    if (isDownAction) {
                        MouseInputModeUtil.tryEnter()
                    } else if (isRelease) {
                        MouseInputModeUtil.exit()
                    }
                }

                else if isDownAction -> when (event.key) {
                    AllRegisters.KeyMapping.collapseKey.key.value -> HologramManager.trySwitchWidgetCollapse()
                    AllRegisters.KeyMapping.pingScreenKey.key.value -> HologramManager.tryPingInteractScreen()
                    AllRegisters.KeyMapping.pingVectorKey.key.value -> HologramManager.tryPingInteractVector()
                }
            }

            HologramInteractionManager.onKey(event)
        }

        private fun onMouseScroll(event: InputEvent.MouseScrollingEvent) {
            if (Minecraft.getInstance().level == null) return
            if (Minecraft.getInstance().screen != null) return

            val shiftDown = GLFW.glfwGetKey(Minecraft.getInstance().window.window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            val modifier = if (shiftDown) 0.05 else 0.2
            val changeValue = event.scrollDeltaY * modifier

            run {
                val modifyTarget = PanelOperatorManager.modifyTarget ?: return@run
                val player = Minecraft.getInstance().player ?: return@run
                val window = Minecraft.getInstance().window.window
                val locate = modifyTarget.locate as? LocateType.World.FacingVector ?: return@run

                val axisMode = PanelOperatorManager.axisMode
                val vector = when (GLFW.GLFW_PRESS) {
                    GLFW.glfwGetKey(window, GLFW.GLFW_KEY_X) -> axisMode.extractX(player, locate)
                    GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Y) -> axisMode.extractY(player, locate)
                    GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Z) -> axisMode.extractZ(player, locate)
                    else -> return@run
                }
                locate.offset.add(vector.mul(changeValue.toFloat()))
                event.isCanceled = true
                return
            }

            HologramManager.getInteractHologram()?.also { state ->
                val locate = state.locate as? LocateType.World.FacingVector ?: return@also

                locate.scale = max(min(max(locate.scale + changeValue.toFloat(), 0.01f), 2.5f), 0.2f)
                Minecraft.getInstance().gui.setOverlayMessage(Component.literal("adjust hologram scale to %.2f".format(locate.scale)), false)

                event.isCanceled = true
                return
            }

            if (AllRegisters.KeyMapping.scaleKey.isDown) {
                val scale = Config.Client.globalHologramScale
                scale.setAndSave(min(max(scale.get() + changeValue, 0.01), 2.5))
                Minecraft.getInstance().gui.setOverlayMessage(Component.literal("adjust global scale to %.2f".format(scale.get())), false)
                event.isCanceled = true
                return
            }

            if (HologramInteractionManager.onMouseScroll(event)) {
                event.isCanceled = true
            }
        }

        private fun onMouseButton(event: InputEvent.MouseButton.Pre) {
            if (MouseInputModeUtil.preventPlayerTurn()) {
                event.isCanceled = true
            }

            if (CycleSelector.instanceExist()) {
                if (event.action == GLFW.GLFW_PRESS) {
                    CycleSelector.onClick()
                }
                return
            }

            if (AllRegisters.KeyMapping.freeMouseMoveKey.isDown) {
                if (event.button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && event.action == GLFW.GLFW_PRESS) {
                    HologramManager.getInteractHologram()?.widget?.closeWidget()
                    event.isCanceled = true
                }
            }

            if (Minecraft.getInstance().level == null) return
            if (Minecraft.getInstance().screen != null) return

            if (HologramInteractionManager.onMouseClick(event)) {
                event.isCanceled = true
            }
        }

        private fun onInteraction(event: InputEvent.InteractionKeyMappingTriggered) {
//            event.isCanceled = true
        }

        private fun registerLayer(event: RegisterGuiLayersEvent) {
            event.registerBelow(
                VanillaGuiLayers.CROSSHAIR, HologramPanel.rl("interaction_mode_layer"), InteractionLayer.getLayer()
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
            val pose = event.poseStack
            pose.stack {
                DebugHelper.Client.renderLevelLast(event)
            }
            pose.stack {
                HologramManager.renderWorldPart(event)
            }
        }
    }

    private fun registerPayload(event: RegisterPayloadHandlersEvent) {
        event.registrar("1.0").playToClient(
            ServerHandShakePayload.TYPE, ServerHandShakePayload.STREAM_CODEC, ServerHandShakePayload.HANDLE
        ).playToServer(
            ComponentRequestDataPayload.TYPE, ComponentRequestDataPayload.STREAM_CODEC, ComponentRequestDataPayload.HANDLE
        ).playToClient(
            ComponentResponseDataPayload.TYPE, ComponentResponseDataPayload.STREAM_CODEC, ComponentResponseDataPayload.HANDLE
        ).playBidirectional(
            SyncClosePayload.TYPE, SyncClosePayload.STREAM_CODEC, SyncClosePayload.HANDLE
        ).playToClient(
            EntityConversationPayload.TYPE, EntityConversationPayload.STREAM_CODEC, EntityConversationPayload.HANDLE
        ).playToServer(
            QueryDebugStatisticsPayload.TYPE, QueryDebugStatisticsPayload.STREAM_CODEC, QueryDebugStatisticsPayload.HANDLE
        ).playToClient(
            DebugStatisticsPayload.TYPE, DebugStatisticsPayload.STREAM_CODEC, DebugStatisticsPayload.HANDLE
        ).playToServer(
            ItemInteractivePayload.TYPE, ItemInteractivePayload.STREAM_CODEC, ItemInteractivePayload.HANDLE
        ).playBidirectional(
            MimicPayload.TYPE, MimicPayload.STREAM_CODEC, MimicPayload.HANDLE
        ).playToServer(
            TransTargetPayload.TYPE, TransTargetPayload.STREAM_CODEC, TransTargetPayload.HANDLE
        )
    }

    private fun registerCommand(event: RegisterCommandsEvent) {
        CommandDSL(event.dispatcher).apply {
            HologramPanel.MOD_ID {
                if (HologramPanel.underDebug) {
                    "froze_time_and_weather" {
                        execute {
                            val server = ServerLifecycleHooks.getCurrentServer() ?: return@execute
                            server.commands.performPrefixedCommand(source, "gamerule doDaylightCycle false")
                            server.commands.performPrefixedCommand(source, "gamerule doWeatherCycle false")
                        }
                    }
                    "clear_all_items" {
                        execute {
                            val server = ServerLifecycleHooks.getCurrentServer() ?: return@execute
                            server.commands.performPrefixedCommand(source, "kill @e[type=minecraft:item]")
                        }
                    }
                    "data_sync_interval" {
                        "interval"(IntegerArgumentType.integer(1, 20)) {
                            execute {
                                val interval = IntegerArgumentType.getInteger(this, "interval")
                                Config.Server.updateInternal.set(interval)
                            }
                        }
                    }
                    "allow_hologram_interactive" {
                        "allow"(BoolArgumentType.bool()) {
                            execute {
                                val allow = BoolArgumentType.getBool(this, "allow")
                                Config.Server.allowHologramInteractive.set(allow)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun registerClientCommand(event: RegisterClientCommandsEvent) {
        CommandDSL(event.dispatcher).apply {
            HologramPanel.MOD_ID {
                "debug" {
                    "debug_layer" {
                        execute {
                            val newState = Config.Client.renderDebugLayer.switchAndSave()
                            source.sendSystemMessage(Component.literal("switch debug_layer state to $newState"))
                        }
                    }
                    "debug_lifecycle" {
                        execute {
                            val newState = Config.Client.renderDebugHologramLifeCycleBox.switchAndSave()
                            source.sendSystemMessage(Component.literal("switch debug_lifecycle state to $newState"))
                        }
                    }
                    "debug_widget" {
                        execute {
                            val newState = Config.Client.renderWidgetDebugInfo.switchAndSave()
                            source.sendSystemMessage(Component.literal("switch debug_widget state to $newState"))
                        }
                    }
                    "debug_network_usage" {
                        execute {
                            val newState = Config.Client.renderNetworkDebugInfo.switchAndSave()
                            source.sendSystemMessage(Component.literal("switch debug_network_usage state to $newState"))
                        }
                    }
                    "debug_transient_target" {
                        execute {
                            val newState = Config.Client.renderDebugTransientTarget.switchAndSave()
                            source.sendSystemMessage(Component.literal("switch debug_transient state to $newState"))
                        }
                    }
                    "render_interact_transient_re_mapping_indicator" {
                        execute {
                            val newState = Config.Client.renderInteractTransientReMappingIndicator.switchAndSave()
                            source.sendSystemMessage(Component.literal("switch render_interact_transient_re_mapping_indicator state to $newState"))
                        }
                    }
                    "invalidate_cache" {
                        execute {
                            PluginManager.ProviderManager.invalidateCache()
                        }
                    }
                }
                "global_hologram_scale" {
                    "scale"(DoubleArgumentType.doubleArg(0.01, 20.0)) {
                        execute {
                            val value = DoubleArgumentType.getDouble(this, "scale")
                            Config.Client.globalHologramScale.setAndSave(value)
                            source.sendSystemMessage(Component.literal("set global hologram state to $value"))
                        }
                    }
                }
                "clear_all_widget" {
                    execute {
                        HologramManager.clearAllHologram()
                    }
                }
                "clear_all_cache" {
                    execute {
                        PluginManager.ProviderManager.invalidateCache()
                    }
                }
                "search" {
                    "str"(StringArgumentType.string()) {
                        execute {
                            val str = StringArgumentType.getString(this, "str")
                            SearchBackend.getCurrentBackend().setSearchString(str)
                            val style = Style.EMPTY.withClickEvent(
                                ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hologram_panel search")
                            ).withHoverEvent(
                                HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("click to clear"))
                            )
                            source.sendSystemMessage(Component.literal("set search string:${str}").withStyle(style))
                        }
                    }

                    execute {
                        SearchBackend.getCurrentBackend().setSearchString("")
                        source.sendSystemMessage(Component.literal("clear search string"))
                    }

                    "backend" {
                        "type"(EnumArgument.enumArgument(SearchBackend.Type::class.java)) {
                            execute {
                                val type = getArgument("type", SearchBackend.Type::class.java)
                                Config.Client.searchBackend.setAndSave(type)
                            }
                        }

                        execute {
                            val backend = Config.Client.searchBackend.get()
                            this.source.sendSystemMessage(Component.literal("current backend:${backend}"))
                        }
                    }

                    "display" {
                        execute {
                            val str = SearchBackend.getCurrentBackend().getSearchString()
                            if (str != null && str.isNotEmpty()) {
                                this.source.sendSystemMessage(Component.literal("search string:${str}"))
                            } else {
                                this.source.sendSystemMessage(Component.literal("No search text set"))
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
            HologramManager.clearAllHologram()
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
        PluginManager.getInstance().onClientRegisterEnd()
    }

    private fun onLoadComplete(event: FMLLoadCompleteEvent) {

    }

    /**
     * we use this event to know that all registries have frozen
     */
    fun onRegistryEnd() {
        PluginManager.ProviderManager.collectProvidersFromRegistry()
        PluginManager.getInstance().registerPluginConfigs()
    }

    private fun onEntityJoinLevel(event: EntityJoinLevelEvent) {
        val level = event.level
        if (level.isClientSide) {
            EntityConversationPayload.onEntityJoin(event.entity)
        }
    }

    private fun onMobConversion(event: LivingConversionEvent.Post) {
        val old = event.entity
        val new = event.outcome
        val payload = EntityConversationPayload(old.id, new.id, new.level().dimension())
        PacketDistributor.sendToPlayersTrackingEntity(old, payload)
    }
}
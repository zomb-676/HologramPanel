package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.api.IServerDataRequester
import com.github.zomb_676.hologrampanel.payload.CloseRequestWidgetPayload
import com.github.zomb_676.hologrampanel.payload.ComponentWidgetQueryPayload
import com.github.zomb_676.hologrampanel.payload.ComponentWidgetResponsePayload
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

object HologramComponentWidgetRequesterManager {
    object Client {
        val widgetUUID: MutableMap<HologramComponentWidget<*>, UUID> = mutableMapOf()
        val UUIDComponent: MutableMap<UUID, List<IServerDataRequester<*>>> = mutableMapOf()

        fun createRequest(
            context: ContextHolder,
            requesters: List<IServerDataRequester<*>>,
            widget: HologramComponentWidget<*>
        ) {
            val player = Minecraft.getInstance().player ?: return
            val uuid = UUID.randomUUID()

            widgetUUID[widget] = uuid
            UUIDComponent[uuid] = requesters

            val payload = ComponentWidgetQueryPayload(uuid, context, requesters.map(IServerDataRequester<*>::getProcessor))
            player.connection.send(payload)
        }

        fun closeWidget(widget: HologramComponentWidget<*>) {
            val uuid = widgetUUID.remove(widget) ?: return
            UUIDComponent.remove(uuid)!!
            CloseRequestWidgetPayload.close(uuid)
        }

        fun closeForPlayer() {
            widgetUUID.clear()
            UUIDComponent.clear()
        }

        fun handle(uuid: UUID, buf: RegistryFriendlyByteBuf) {
            val requesters = UUIDComponent[uuid]
            if (requesters == null) {
                //sever have a record which doesn't exist on client, so close it
                CloseRequestWidgetPayload.close(uuid)
                return
            }
            requesters.forEach { it.onServerDataReceived(buf) }
        }
    }

    object Server {
        val requesters: MutableMap<ServerPlayer, MutableMap<UUID, ComponentWidgetQueryPayload>> = mutableMapOf()
        private var nextUpdate: Int = 5

        fun createResponse(payload: ComponentWidgetQueryPayload, context: IPayloadContext) {
            val player = context.player() as ServerPlayer
            requesters.computeIfAbsent(player) { _ -> mutableMapOf() }[payload.widgetUUID] = payload
        }

        fun tick(event: ServerTickEvent) {
            if (--nextUpdate != 0) {
                return
            } else {
                nextUpdate = 5
            }
            requesters.forEach { (player, payloads) ->
                val level: ServerLevel = player.serverLevel()
                payloads.values.forEach { payload ->
                    val buffer = RegistryFriendlyByteBuf(
                        Unpooled.buffer(),
                        player.registryAccess(),
                        player.connection.connectionType
                    )
                    payload.requester.forEach { it.appendServerData(payload.context, buffer, level) }
                    ComponentWidgetResponsePayload.response(player, buffer, payload.widgetUUID)
                    buffer.clear()
                }
            }
        }

        fun onWidgetClose(uuid: UUID, player: ServerPlayer) {
            requesters[player]?.remove(uuid)
        }

        fun closeForPlayer(serverPlayer: ServerPlayer) {
            requesters.remove(serverPlayer)
        }
    }
}
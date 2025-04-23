package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import com.github.zomb_676.hologrampanel.util.IgnorePacketException
import com.github.zomb_676.hologrampanel.util.unsafeCast
import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import java.util.*
import java.util.function.Supplier


object NetworkHandle {

    private const val VERSION = "1"

    private val channel = NetworkRegistry.newSimpleChannel(
        HologramPanel.rl("network"), { VERSION },
        VERSION::equals, VERSION::equals
    )
    private val directions: MutableMap<Class<*>, NetworkDirection> = mutableMapOf()

    fun sendToServer(packet: CustomPacketPayload<*>) = channel.sendToServer(packet)
    fun <T> send(packet: CustomPacketPayload<T>, connection: Connection) =
        channel.sendTo(packet, connection, directions[packet::class.java])

    fun sendTo(packet: CustomPacketPayload<*>, packetTarget: PacketDistributor.PacketTarget) =
        channel.send(packetTarget, packet)

    @Suppress("INACCESSIBLE_TYPE")
    private fun <T> registerEntry(entry: MessageEntry<T>) {
        channel.registerMessage(
            entry.index, entry.type, entry::encode, entry::decode, entry::handle,
            Optional.ofNullable(entry.direction())
        )
    }

    /**
     * further warp
     *
     * [FriendlyByteBuf]'s first byte is used to store [MessageEntry.index]
     *
     * * direction assert if specified
     * * [NetworkEvent.Context.packetHandled] set true
     * * [FriendlyByteBuf.writerIndex] and [FriendlyByteBuf.readerIndex] check
     * at [MessageEntry.decode] and [MessageEntry.encode]
     */
    private inline fun <reified T : CustomPacketPayload<T>> register(
        decodeFunction: StreamCodec<RegistryFriendlyByteBuf, T>,
        direction: NetworkDirection? = null,
    ) {
        registerEntry(object : MessageEntry<T>(T::class.java) {
            override fun encode(instance: T, buf: FriendlyByteBuf) {
                decodeFunction.encode(buf, instance)
            }

            override fun decode(buf: FriendlyByteBuf): T {
                return try {
                    decodeFunction.decode(buf)
                } catch (_: IgnorePacketException) {
                    MimicPayload.unsafeCast()
                }
            }

            override fun handle(instance: T, context: Supplier<NetworkEvent.Context>) {
                @Suppress("NAME_SHADOWING") val context = context.get()
                try {
                    context.enqueueWork {
                        instance.handle(context)
                    }
                } catch (e: Exception) {
                    val message = "error while handle packet for ${T::class.simpleName}"
                    HologramPanel.LOGGER.error(message, e)
                }
                context.packetHandled = true
            }

            override fun direction(): NetworkDirection? = direction
        })
        if (direction != null) {
            directions[T::class.java] = direction
        }
    }

    fun registerPackets() {
        register(
            ComponentRequestDataPayload.STREAM_CODEC
                    as StreamCodec<RegistryFriendlyByteBuf, ComponentRequestDataPayload<Any>>
        )
        register(ComponentResponseDataPayload.STREAM_CODEC)
        register(DebugStatisticsPayload.STREAM_CODEC)
        register(EntityConversationPayload.STREAM_CODEC)
        register(ItemInteractivePayload.STREAM_CODEC)
        register(MimicPayload.STREAM_CODEC)
        register(QueryDebugStatisticsPayload.STREAM_CODEC)
        register(ServerHandShakePayload.STREAM_CODEC)
        register(SyncClosePayload.STREAM_CODEC)
        register(
            TransTargetPayload.STREAM_CODEC
                    as StreamCodec<RegistryFriendlyByteBuf, TransTargetPayload<Any, Any>>
        )
    }
}
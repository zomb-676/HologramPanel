package com.github.zomb_676.hologrampanel.sync

import com.github.zomb_676.hologrampanel.payload.DataSynchronizerSyncPayload
import com.github.zomb_676.hologrampanel.widget.interactive.DistType
import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.neoforged.neoforge.common.extensions.ICommonPacketListener
import java.util.*
import kotlin.reflect.KProperty

class DataSynchronizer(
    val uuid: UUID,
    val distType: DistType,
    val connection: ICommonPacketListener,
    val registryAccess: RegistryAccess
) {

    private val handles = mutableListOf<Handle<*>>()

    class Handle<T>(val target: SynchronizedData<T>, val codec: StreamCodec<in RegistryFriendlyByteBuf, T>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = target.getValue()
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = target.setValue(value)

        fun get(): T = target.getValue()
        fun set(value: T) = target.setValue(value)

        fun addListener(code: (T) -> Unit): Handle<T> {
            return this
        }

        fun notifyListener(): Handle<T> {
            return this
        }

        fun syncIfNecessary(): Handle<T> {
            return this
        }
    }

    fun tick() {
        var buffer = lazy {
            val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), this.registryAccess, this.connection.connectionType)
            buf.writeShort(0)
            buf
        }

        var count: Int = 0

        this.handles.forEachIndexed { index, handle ->
            fun <T> doFor(handle: Handle<T>) {
                val target = handle.target
                if (target.updateIfNecessary()) {
                    target.update()
                }
                if (target.consumeDirty()) {
                    val buff = buffer.value
                    val syncData = target.getValueByPass() ?: return
                    buff.writeVarInt(index)
                    handle.codec.encode(buff, syncData)
                    count++
                }
            }
            doFor(handle)
        }
        if (count != 0) {
            buffer.value.setShort(0, count)
            val payload = DataSynchronizerSyncPayload(this.uuid, buffer.value.asByteBuf().array())
            this.connection.send(payload)
        }
    }

    fun onSyncDataReceived(buf: RegistryFriendlyByteBuf) {
        fun <T> doFor(handle: Handle<T>) {
            val data = handle.codec.decode(buf)
            handle.target.setValueByPass(data)
        }

        val count = buf.readShort()
        repeat(count.toInt()) { _ ->
            val id = buf.readVarInt()
            val handle = handles[id]
            doFor(handle)
        }
    }

    fun <T> SynchronizedData<T>.handle(streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>): Handle<T> {
        val handle = Handle(this, streamCodec)
        handles.add(handle)
        return handle
    }

    inline fun <T> queryFromServerPerTick(
        initial: T,
        streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>,
        crossinline queryFunction: () -> T
    ): Handle<T> {
        return when (distType) {
            DistType.SERVER -> object : SynchronizedData<T>(initial) {
                override fun updateIfNecessary() = true
                override fun update() {
                    val value = queryFunction.invoke()
                    if (value != this.data) {
                        this.data = value
                        this.markDirty()
                    }
                }

                override fun setValue(value: T) = throw RuntimeException()
            }

            DistType.CLIENT -> object : SynchronizedData<T>(initial) {
                override fun setValue(value: T) = throw RuntimeException()
            }
        }.handle(streamCodec)
    }
}
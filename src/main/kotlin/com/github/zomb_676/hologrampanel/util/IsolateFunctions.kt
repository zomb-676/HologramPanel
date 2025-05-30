package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import io.netty.buffer.ByteBuf
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.profiling.Profiler
import net.minecraft.util.profiling.ProfilerFiller
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.ModConfigSpec
import net.neoforged.neoforge.common.NeoForge
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.StampedLock
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Suppress("UNCHECKED_CAST")
fun <T> Any.unsafeCast(): T = this as T

fun <T> Any.unsafeCast(errorString: String): T {
    try {
        return this.unsafeCast<T>()
    } catch (e: ClassCastException) {
        throw RuntimeException(errorString, e)
    }
}

/**
 * @param code must be crossinline to call the paired pop
 */
inline fun PoseStack.stack(crossinline code: () -> Unit) {
    this.pushPose()
    code.invoke()
    this.popPose()
}

inline fun GuiGraphics.stack(crossinline code: () -> Unit) {
    this.pose().stack(code)
}

inline fun HologramStyle.stack(crossinline code: () -> Unit) {
    this.guiGraphics.stack(code)
}

fun mainCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

typealias JomlMath = org.joml.Math

inline fun <reified T> isInstanceOf(claz: Class<*>) = T::class.java.isAssignableFrom(claz)

inline fun <reified T> requireInstanceOf(claz: Class<*>): Class<out T> {
    require(isInstanceOf<T>(claz))
    return claz.unsafeCast()
}

inline fun <reified T> getClassOf(className: String): Class<out T> =
    requireInstanceOf<T>(Class.forName(className, false, Thread.currentThread().contextClassLoader))

inline val profiler: ProfilerFiller get() = Profiler.get()

/**
 * @param code must be crossinline, as the paired pop must be called
 */
inline fun <T> profilerStack(name: String, crossinline code: () -> T): T {
    profiler.push(name)
    val res = code.invoke()
    profiler.pop()
    return res
}

inline val Double.normalizedInto2PI: Double
    get() {
        if (this == (Math.PI * 2)) return this
        val res = this % (Math.PI * 2)
        return if (res < 0) {
            res + (Math.PI * 2)
        } else {
            res
        }
    }

inline fun glDebugStack(debugLabelName: String, id: Int = 0, crossinline code: () -> Unit) {
    if (GL.getCapabilities().GL_KHR_debug) {
        GL46.glPushDebugGroup(GL46.GL_DEBUG_SOURCE_APPLICATION, id, debugLabelName)
        code.invoke()
        GL46.glPopDebugGroup()
    } else {
        code.invoke()
    }
}

inline fun ModConfigSpec.Builder.stack(name: String, crossinline f: () -> Unit) {
    this.push(name)
    f.invoke()
    this.pop()
}

fun ByteBuf.extractArray(): ByteArray {
    val percent = this.writerIndex().toFloat() / this.capacity()
    if (percent < 0.8 || (1 - percent) > 512) {
        return this.array().sliceArray(0..<this.writerIndex())
    }
    return this.array()
}

fun ModConfigSpec.BooleanValue.switch(): Boolean {
    val state = !this.get()
    this.set(state)
    return state
}

fun ModConfigSpec.BooleanValue.switchAndSave(): Boolean {
    val state = !this.get()
    this.setAndSave(state)
    return state
}

private object ConfigSaveHelper {
    val hasSetTask = AtomicBoolean(false)
    val lock = StampedLock()
    val saveTasks: MutableSet<ModConfigSpec.ConfigValue<*>> = ConcurrentHashMap.newKeySet()

    fun scheduleTask() {
        val stamp = lock.tryWriteLock()
        if (stamp == 0L) {
            hasSetTask.set(false)
            return
        }

        try {
            synchronized(saveTasks) {
                val copy = saveTasks.toSet()
                saveTasks.clear()
                copy
            }.forEach { value ->
                try {
                    value.save()
                } catch (e: Throwable) {
                    HologramPanel.LOGGER.debug("config: path({}), value:{} failed to save", value.path.joinToString(), value.get())
                    HologramPanel.LOGGER.debug("Error while saving config", e)
                    Minecraft.getInstance().gui.chat.addMessage(Component.literal("failed to save config, see log for detailed information"))
                }
            }
        } finally {
            lock.unlockWrite(stamp)
            hasSetTask.set(false)
            if (saveTasks.isNotEmpty()) {
                scheduleTask()
            }
        }
    }

    fun save(value: ModConfigSpec.ConfigValue<*>) {
        saveTasks.add(value)

        if (hasSetTask.compareAndSet(false, true)) {
            Minecraft.getInstance().schedule(::scheduleTask)
        }
    }
}

fun <T : Any> ModConfigSpec.ConfigValue<T>.setAndSave(value: T) {
    this.set(value)
    ConfigSaveHelper.save(this)
}

inline fun <T : Any> ModConfigSpec.ConfigValue<T>.modifyAndSave(code: (T) -> Unit) {
    code.invoke(this.get())
    this.setAndSave(this.get())
}

/**
 * use the [container] to reduce object allocation during the context scope
 */
context(container: Vector3f) fun VertexConsumer.vertex(matrix4f: Matrix4f, x: Float, y: Float, z: Float): VertexConsumer {
    matrix4f.transformPosition(x, y, z, container)
    return this.addVertex(container.x, container.y, container.z)
}

fun timeInterpolation(value: Int): Double {
    val currentTime = System.currentTimeMillis() / 1000.0 * 3
    val period = max(value * 0.5, 3.0)
    val wave = sin((Math.PI / 2) * cos(2 * Math.PI * currentTime / period)).let { it / 2 + 0.5 }
    return JomlMath.lerp(wave, 0.0, value.toDouble())
}

fun <T : Event> T.dispatch(bus: IEventBus): T {
    bus.post(this)
    return this
}

fun <T : Event> T.dispatchForge() = dispatch(NeoForge.EVENT_BUS)

fun addClientMessage(message: Component) = Minecraft.getInstance().gui.chat.addMessage(message)

fun addClientMessage(message: String) = addClientMessage(Component.literal(message))

fun setOverlayMessage(messages: Component) {
    Minecraft.getInstance().gui.setOverlayMessage(messages, false)
}
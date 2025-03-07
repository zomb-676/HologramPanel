package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.profiling.Profiler
import net.minecraft.util.profiling.ProfilerFiller
import net.neoforged.neoforge.client.GlStateBackup

@Suppress("UNCHECKED_CAST")
fun <T> Any.unsafeCast(): T = this as T

inline fun PoseStack.stack(code: () -> Unit) {
    this.pushPose()
    code.invoke()
    this.popPose()
}

inline fun GuiGraphics.stack(code: () -> Unit) {
    this.pose().stack(code)
}

inline fun HologramStyle.stack(code: () -> Unit) {
    this.guiGraphics.stack(code)
}

inline fun HologramStyle.stackIf(check: Boolean, addition: () -> Unit, code: () -> Unit) {
    if (check) {
        this.stack {
            addition.invoke()
            code.invoke()
        }
    } else {
        code.invoke()
    }
}

fun mainCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

typealias JomlMath = org.joml.Math

inline fun <reified T> isInstanceOf(claz: Class<*>) = T::class.java.isAssignableFrom(claz)

inline fun <reified T> requireInstanceOf(claz: Class<*>): Class<out T> {
    require(isInstanceOf<T>(claz))
    return claz.unsafeCast()
}

inline fun <reified T> getClassOf(className: String): Class<out T> =
    requireInstanceOf<T>(Class.forName(className))

inline fun stackRenderState(state: GlStateBackup = GlStateBackup(), code: () -> Unit) {
    RenderSystem.backupGlState(state)
    code.invoke()
    RenderSystem.restoreGlState(state)
}

inline val profiler: ProfilerFiller get() = Profiler.get()
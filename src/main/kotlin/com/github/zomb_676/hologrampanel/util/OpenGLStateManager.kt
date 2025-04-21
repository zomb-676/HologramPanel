package com.github.zomb_676.hologrampanel.util

object OpenGLStateManager {

    /**
     * prevent [net.minecraft.client.renderer.RenderStateShard.OutputStateShard]
     *
     * sometimes we need to render something into another target
     */
    @PublishedApi
    @JvmField
    internal var preventMainBindWrite : Boolean = false

    inline fun preventMainBindWrite(block: () -> Unit) {
        preventMainBindWrite = true
        block()
        preventMainBindWrite = false
    }
}
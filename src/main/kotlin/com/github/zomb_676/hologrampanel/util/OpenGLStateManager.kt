package com.github.zomb_676.hologrampanel.util

object OpenGLStateManager {

    @PublishedApi
    @JvmField
    internal var preventMainBindWrite : Boolean = false

    inline fun preventMainBindWrite(block: () -> Unit) {
        preventMainBindWrite = true
        block()
        preventMainBindWrite = false
    }
}
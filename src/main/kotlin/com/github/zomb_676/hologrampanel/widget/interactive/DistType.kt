package com.github.zomb_676.hologrampanel.widget.interactive

import net.minecraft.world.level.Level

/**
 * logic side
 */
enum class DistType {
    CLIENT, SERVER;

    companion object {
        fun from(level: Level) = when (level.isClientSide) {
            true -> CLIENT
            false -> SERVER
        }
    }

    inline val isClientSide get() = this == CLIENT
    inline val isServerSide get() = this == SERVER

    inline fun runOnClient(code: () -> Unit) {
        if (isClientSide) {
            code.invoke()
        }
    }

    inline fun runOnServer(code: () -> Unit) {
        if (isServerSide) {
            code.invoke()
        }
    }
}
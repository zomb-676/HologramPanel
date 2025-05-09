package com.github.zomb_676.hologrampanel.util

import org.jetbrains.annotations.ApiStatus
import java.util.function.IntSupplier

/**
 * reduce pattern code, do an actual tick by every [interval] for better performance
 */
class AutoTicker private constructor(val interval: IntSupplier) {
    companion object {
        fun by(interval: IntSupplier) = AutoTicker(interval)
        fun by(interval: Int) = AutoTicker { interval }
    }

    @PublishedApi
    @ApiStatus.Internal
    internal var tick = interval.asInt

    inline fun tryConsume(code: () -> Unit) {
        if (--tick == 0) {
            tick = interval.asInt
            code.invoke()
        }
    }

    inline operator fun invoke(f: () -> Unit) = tryConsume(f)
}
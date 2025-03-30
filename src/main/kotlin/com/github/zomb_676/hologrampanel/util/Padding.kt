package com.github.zomb_676.hologrampanel.util

data class Padding(val left: Int, val right: Int, val up: Int, val down: Int) {
    constructor(length: Int) : this(length, length, length, length)

    inline val horizontal get() = left + right
    inline val vertical get() = up + down
}
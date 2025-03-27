package com.github.zomb_676.hologrampanel.util

class ProgressData(var progressCurrent: Int = 0, var progressMax: Int = 100, val LTR: Boolean = true) {
    val percent get() = progressCurrent.toFloat() / progressMax

    fun current(value: Int): ProgressData {
        this.progressCurrent = value
        return this
    }

    fun max(value: Int): ProgressData {
        this.progressMax = value
        return this
    }

    override fun toString(): String {
        return "(current=$progressCurrent, max=$progressMax, LTR=$LTR)"
    }


}
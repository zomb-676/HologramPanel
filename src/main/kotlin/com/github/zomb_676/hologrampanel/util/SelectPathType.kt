package com.github.zomb_676.hologrampanel.util

@JvmInline
value class SelectPathType private constructor(@PublishedApi internal val value: Int) {
    companion object {
        fun <T> of(path: SelectedPath<T>, any: T): SelectPathType = if (path.atTerminus(any)) {
            if (path.atHead(any)) {
                SelectPathType(HEAD_MASK or TERMINAL_MASK)
            } else {
                SelectPathType(TERMINAL_MASK)
            }
        } else if (path.atHead(any)) {
            SelectPathType(HEAD_MASK)
        } else if (path.atUnTerminusPath(any)) {
            SelectPathType(NONE_END_MASK)
        } else {
            SelectPathType(0)
        }

        fun ofCandidate(isCandidate: Boolean) = if (isCandidate) {
            SINGLE_TREE
        } else {
            EMPTY
        }

        const val HEAD_MASK = 0x01 shl 0
        const val NONE_END_MASK = 0x01 shl 1
        const val TERMINAL_MASK = 0x01 shl 2

        val EMPTY = SelectPathType(0)
        val SINGLE_TREE = SelectPathType(HEAD_MASK and TERMINAL_MASK)
    }

    inline val isUnSelect get() = this.value == 0
    inline val isOnWholePath get() = this.value != 0

    inline val isAtHead get() = (value and HEAD_MASK) != 0
    inline val isAtNoneEnd get() = (value and NONE_END_MASK) != 0
    inline val isAtTerminal get() = (value and TERMINAL_MASK) != 0

    inline val isAtNoneTerminalPath get() = (value and (HEAD_MASK or NONE_END_MASK)) != 0
    inline val isAtNoneHeadPath get() = (value and (TERMINAL_MASK or NONE_END_MASK)) != 0
}
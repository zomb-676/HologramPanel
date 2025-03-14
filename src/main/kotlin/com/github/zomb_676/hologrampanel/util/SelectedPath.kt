package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent

/**
 * used for object selection in tree like data structure
 */
interface SelectedPath<T> {
    fun atTerminus(component: T): Boolean
    fun atUnTerminusPath(component: T): Boolean
    fun atWholePath(component: T): Boolean =
        atTerminus(component) || atUnTerminusPath(component)

    fun unTerminalPath(): Sequence<T>
    fun terminal(): T
    fun fullPath(): Sequence<T> = sequence {
        yieldAll(unTerminalPath())
        yield(terminal())
    }

    fun forAny(any: T): SelectPathType =
        if (atTerminus(any)) {
            SelectPathType.ON_TERMINAL
        } else if (atUnTerminusPath(any)) {
            SelectPathType.ON_NONE_TERMINAL_PATH
        } else {
            SelectPathType.UN_SELECTED
        }

    fun forTerminal(terminal: T): SelectPathType =
        if (atTerminus(terminal)) SelectPathType.ON_TERMINAL else SelectPathType.UN_SELECTED

    fun resetToDefault()

    class Empty<T : Any>(val terminal: HologramWidgetComponent<T>) : SelectedPath<HologramWidgetComponent<T>> {

        override fun atTerminus(component: HologramWidgetComponent<T>): Boolean = false

        override fun atUnTerminusPath(component: HologramWidgetComponent<T>): Boolean = false

        override fun unTerminalPath(): Sequence<HologramWidgetComponent<T>> = emptySequence()

        override fun terminal(): HologramWidgetComponent<T> = terminal

        override fun atWholePath(component: HologramWidgetComponent<T>): Boolean = false

        override fun fullPath(): Sequence<HologramWidgetComponent<T>> = sequenceOf(terminal)

        override fun forAny(any: HologramWidgetComponent<T>): SelectPathType = SelectPathType.UN_SELECTED

        override fun forTerminal(terminal: HologramWidgetComponent<T>): SelectPathType = SelectPathType.UN_SELECTED

        override fun resetToDefault() {}
    }
}
package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent

/**
 * used for object selection in tree like data structure
 */
interface SelectedPath<T> {
    fun atTerminus(component: T): Boolean
    fun atUnTerminusPath(component: T): Boolean
    fun atWholePath(component: T): Boolean =
        atTerminus(component) || atUnTerminusPath(component)

    fun atHead(component: T): Boolean

    fun unTerminalPath(): Sequence<T>
    fun terminal(): T
    fun fullPath(): Sequence<T> = sequence {
        yieldAll(unTerminalPath())
        yield(terminal())
    }

    fun forAny(any: T): SelectPathType = SelectPathType.of(this, any)

    fun resetToDefault()

    fun tryRecover(newTop: T, oldContents: List<T>)

    fun selectCommand(state: HologramRenderState, command: InteractionCommand.Exact.SelectComponent)

    class Empty<T : Any>(private var terminal: HologramWidgetComponent<T>) : SelectedPath<HologramWidgetComponent<T>> {

        override fun atTerminus(component: HologramWidgetComponent<T>): Boolean = false

        override fun atUnTerminusPath(component: HologramWidgetComponent<T>): Boolean = false

        override fun unTerminalPath(): Sequence<HologramWidgetComponent<T>> = emptySequence()

        override fun terminal(): HologramWidgetComponent<T> = terminal

        override fun atWholePath(component: HologramWidgetComponent<T>): Boolean = false

        override fun atHead(component: HologramWidgetComponent<T>): Boolean = terminal == component

        override fun fullPath(): Sequence<HologramWidgetComponent<T>> = sequenceOf(terminal)

        override fun forAny(any: HologramWidgetComponent<T>): SelectPathType = TODO()

        override fun resetToDefault() {}

        override fun tryRecover(newTop: HologramWidgetComponent<T>, oldContents: List<HologramWidgetComponent<T>>) {
            this.terminal = newTop
        }

        override fun selectCommand(state: HologramRenderState, command: InteractionCommand.Exact.SelectComponent) {}
    }
}
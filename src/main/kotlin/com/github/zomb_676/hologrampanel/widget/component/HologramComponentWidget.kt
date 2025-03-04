package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramState
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand.Exact.SelectComponent
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.SelectedPath
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.widget.HologramWidget

abstract class HologramComponentWidget<T : Any>(val target: T) : HologramWidget() {

    private class SelectTree<T : Any>(val widget: HologramComponentWidget<T>) :
        SelectedPath<HologramWidgetComponent<T>> {
        private val stack = mutableListOf<HologramWidgetComponent.Group<T>>(widget.component)
        private var current: HologramWidgetComponent<T> = widget.component.children.first()
        private var currentIndex = 0
        private var currentDepth = 1

        fun selectCommand(state: HologramState, selectCommand: SelectComponent) {
            when (selectCommand) {
                SelectComponent.SELECT_NEXT -> {
                    val children = stack.lastOrNull()?.children ?: return
                    this.currentIndex = (this.currentIndex + 1) % children.size
                    this.current = children[this.currentIndex]
                }

                SelectComponent.SELECT_BEFORE -> {
                    val children = stack.lastOrNull()?.children ?: return
                    --this.currentIndex
                    this.currentIndex = if (this.currentIndex < 0) {
                        children.size - 1
                    } else {
                        this.currentIndex % children.size
                    }
                    this.current = children[this.currentIndex]
                }

                SelectComponent.SELECT_GROUP_FIRST_CHILD -> {
                    val current = current
                    if (current is HologramWidgetComponent.Group<T>) {
                        current.collapse = false
                        this.stack.addLast(current)
                        this.current = current.children.first()
                        this.currentIndex = 0
                        ++this.currentDepth
                    }
                }

                SelectComponent.SELECT_PARENT -> {
                    if (this.currentDepth > 0) {
                        this.current = this.stack.removeLast()
                        this.currentIndex = 0
                        --this.currentDepth
                    }
                }
            }
        }

        override fun atUnTerminusPath(component: HologramWidgetComponent<T>): Boolean =
            this.stack.contains(component)

        override fun atTerminus(component: HologramWidgetComponent<T>): Boolean = this.current == component

        override fun atWholePath(component: HologramWidgetComponent<T>): Boolean = when (component) {
            is HologramWidgetComponent.Single<*, *> -> atTerminus(component)
            is HologramWidgetComponent.Group<*> -> atUnTerminusPath(component)
        }

        override fun unTerminalPath(): Sequence<HologramWidgetComponent<T>> = this.stack.asSequence()

        override fun terminal(): HologramWidgetComponent<T> = current

        override fun resetToDefault() {
            this.stack.clear()
            this.stack.add(widget.component)
            this.current = widget.component.children.first()
            this.currentIndex = 0
            this.currentDepth = 1
        }
    }

    protected var component: HologramWidgetComponent.Group<T> = initialComponent()
    private var selectedPath: SelectTree<T> = SelectTree(this)
    private var mimicPath: SelectedPath<HologramWidgetComponent<T>> = SelectedPath.Empty<T>(this.component)

    override fun render(state: HologramState, style: HologramStyle, partialTicks: Float) {
        val path: SelectedPath<HologramWidgetComponent<T>> =
            if (state.isSelected()) this.selectedPath else mimicPath
        this.component.render(style, path, partialTicks)
    }

    override fun measure(displayType: DisplayType, style: HologramStyle): Size {
        this.component = updateComponent(this.component)
        this.component.measureSize(this.target, displayType, style)
        return this.component.visualSize
    }

    protected abstract fun initialComponent(): HologramWidgetComponent.Group<T>

    protected open fun updateComponent(component: HologramWidgetComponent.Group<T>): HologramWidgetComponent.Group<T> =
        component

    fun selectComponent(state: HologramState, selectCommand: SelectComponent) {
        selectedPath.selectCommand(state, selectCommand)
    }

    override fun onSelected() {}
    override fun onDisSelected() {
        selectedPath.resetToDefault()
    }

    fun operateCommand(state: HologramState, operateCommand: InteractionCommand.Exact.OperateCommand) {
        when (operateCommand) {
            InteractionCommand.Exact.OperateCommand.SWITCH_COLLAPSE -> {
                val selected = this.selectedPath.terminal()
                if (selected is HologramWidgetComponent.Group<T>) {
                    selected.switchCollapse()
                }
            }
        }
    }
}
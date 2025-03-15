package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand.Exact.SelectComponent
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.SelectedPath
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget

/**
 * tree structure type widget
 */
abstract class HologramComponentWidget<T : Any>(val target: T, val component: HologramWidgetComponent.Group<T>) :
    HologramWidget {

    override fun render(
        state: HologramRenderState, style: HologramStyle, displayType: DisplayType, partialTicks: Float
    ) {
        val path = this.getSelectedPath()
        this.component.render(target, style, path, displayType, partialTicks)
    }

    abstract fun getSelectedPath() : SelectedPath<HologramWidgetComponent<T>>

    override fun measure(style: HologramStyle, displayType: DisplayType): Size {
        this.component.measureSize(this.target, style, displayType)
        return this.component.visualSize
    }

    fun selectComponent(state: HologramRenderState, selectCommand: SelectComponent) {
        try {
            getSelectedPath().selectCommand(state, selectCommand)
        } catch (e: Throwable) {
            println(e)
        }
    }

    override fun onSelected() {}
    override fun onDisSelected() {
        this.resetSelectState()
    }

    fun resetSelectState() {
        this.getSelectedPath().resetToDefault()
    }

    fun operateCommand(state: HologramRenderState, operateCommand: InteractionCommand.Exact.OperateCommand) {
        when (operateCommand) {
            InteractionCommand.Exact.OperateCommand.SWITCH_COLLAPSE -> {
                val selected = this.getSelectedPath().terminal()
                if (selected is HologramWidgetComponent.Group<T>) {
                    selected.switchCollapse()
                }
            }
        }
    }
}
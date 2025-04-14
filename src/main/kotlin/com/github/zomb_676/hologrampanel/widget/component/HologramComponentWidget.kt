package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget

/**
 * tree structure type widget, made up of [HologramWidgetComponent]
 *
 * the leaf is [HologramWidgetComponent.Single]
 * the node is [HologramWidgetComponent.Group]
 */
abstract class HologramComponentWidget<T : Any>(val target: T, val component: HologramWidgetComponent.Group<T>) :
    HologramWidget {

    override fun render(
        state: HologramRenderState, style: HologramStyle, displayType: DisplayType, partialTicks: Float
    ) {
        this.component.render(target, style, displayType, partialTicks)
    }


    override fun measure(style: HologramStyle, displayType: DisplayType): Size {
        this.component.measureSize(this.target, style, displayType)
        return this.component.visualSize
    }

}
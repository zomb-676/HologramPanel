package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import net.minecraft.network.chat.Component

/**
 * render [Component]
 */
open class ComponentRenderElement(val component: Component, val color: Int = 0) : RenderElement() {
    constructor(string: String, color: Int = 0) : this(Component.literal(string), color)

    override fun measureContentSize(style: HologramStyle): Size {
        return style.measureString(component).scale()
    }

    override fun render(style: HologramStyle, partialTicks: Float) {
        style.drawString(component, color = color)
    }

    override fun toString(): String {
        return "String(component=$component)"
    }
}
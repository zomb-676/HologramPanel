package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size
import net.minecraft.network.chat.Component

open class ComponentRenderElement(val component: Component) : RenderElement() {
    constructor(string: String) : this(Component.literal(string))

    override fun measureContentSize(style: HologramStyle): Size {
        return style.measureString(component).scale()
    }

    override fun render(style: HologramStyle, partialTicks: Float) {
        style.drawString(component)
    }

    override fun toString(): String {
        return "String(component=$component)"
    }
}
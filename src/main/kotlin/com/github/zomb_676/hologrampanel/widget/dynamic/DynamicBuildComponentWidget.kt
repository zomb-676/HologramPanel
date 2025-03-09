package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ScreenPosition
import com.github.zomb_676.hologrampanel.util.SelectedPath
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.stackIf
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidget
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent
import java.util.LinkedList
import kotlin.math.max

sealed interface DynamicBuildComponentWidget<T : HologramContext> : HologramWidgetComponent<T> {
    fun getProvider() : ComponentProvider<T>

    class Single<T : HologramContext>(val provider: ComponentProvider<T>,val elements: List<IRenderElement>) : HologramWidgetComponent.Single<T>(), DynamicBuildComponentWidget<T> {
        private var baseY: Int = 0
        private val padding = 1

        override fun measureSize(
            target: T,
            style: HologramStyle,
            displayType: DisplayType
        ): Size {
            var width = 0
            var height = 0
            this.elements.forEach {
                it.contentSize = it.measureContentSize(style)
                val offset = it.getPositionOffset()
                if (offset == ScreenPosition.ZERO) {
                    width += it.contentSize.width
                    height = max(height, it.contentSize.height)
                } else {
                    width += it.contentSize.width + offset.x
                    if (offset.y < 0) {
                        baseY = max(baseY, -offset.y)
                    }
                    height = max(height, it.contentSize.height + offset.y)
                }
            }
            width += (this.elements.size - 1) * padding
            return Size.of(width, height)
        }

        override fun render(
            target: T,
            style: HologramStyle,
            path: SelectedPath<HologramWidgetComponent<T>>,
            displayType: DisplayType,
            partialTicks: Float
        ) {
            val pathType = path.forTerminal(this)
            if (baseY != 0) {
                style.move(0, baseY)
            }
            this.elements.forEach { element ->
                val offset = element.getPositionOffset()
                if (offset != ScreenPosition.ZERO) {
                    style.move(offset)
                }
                style.stackIf(element.getScale() != 0.0, { style.scale(element.getScale()) }) {
                    element.render(style, partialTicks, pathType)
                }
                val size = element.contentSize
                style.move(size.width + padding, -offset.y)
            }
        }

        override fun getProvider(): ComponentProvider<T> = provider
    }

    class Group<T : HologramContext>(
        val provider: ComponentProvider<T>,
        val descriptionWidget : Single<T>,
        override val children: MutableList<DynamicBuildComponentWidget<T>>
    ) : HologramWidgetComponent.Group<T>(children) , DynamicBuildComponentWidget<T> {
        override fun descriptionSize(
            target: T,
            style: HologramStyle,
            displayType: DisplayType
        ): Size = descriptionWidget.measureSize(target, style, displayType)

        override fun renderGroupDescription(
            target: T,
            style: HologramStyle,
            path: SelectedPath<HologramWidgetComponent<T>>,
            displayType: DisplayType,
            partialTicks: Float
        ) {
            descriptionWidget.render(target, style, path, displayType, partialTicks)
        }

        override fun getProvider(): ComponentProvider<T> = provider
    }
}
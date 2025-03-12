package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.SelectedPath
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.DisplayType
import kotlin.math.max

interface HologramWidgetComponent<T : Any> {
    val contentSize: Size

    /**
     * set by [Group.measureSize]
     */
    val visualSize: Size

    /**
     * @return [contentSize]
     */
    fun measureSize(
        target: T,
        style: HologramStyle,
        displayType: DisplayType
    ): Size

    fun render(
        target: T,
        style: HologramStyle,
        path: SelectedPath<HologramWidgetComponent<T>>,
        displayType: DisplayType,
        partialTicks: Float
    )

    abstract class Single<T : Any> : HologramWidgetComponent<T> {
        final override var contentSize: Size = Size.ZERO
            internal set
        final override var visualSize: Size = contentSize
            internal set
    }

    abstract class Group<T : Any>(open val children: List<HologramWidgetComponent<T>>) : HologramWidgetComponent<T> {
        final override var contentSize: Size = Size.ZERO
            private set
        final override var visualSize: Size = contentSize
            private set

        var collapse: Boolean = false

        final override fun measureSize(
            target: T, style: HologramStyle, displayType: DisplayType
        ): Size {
            var width = 0
            var height = 0
            if (!this.collapse) {
                for (child in children) {
                    when (child) {
                        is Single<T> -> {
                            child.contentSize = child.measureSize(target, style, displayType)
                            child.visualSize = style.mergeOutlineSizeForSingle(child.contentSize)
                        }

                        is Group<T> -> {
                            child.measureSize(target, style, displayType)
                        }
                    }
                    val childSize = child.visualSize
                    width = max(width, childSize.width)
                    height += childSize.height
                }
            }
            this.contentSize = Size.of(width, height)
            this.visualSize = style.mergeOutlineSizeForGroup(
                this.contentSize, this.descriptionSize(target, style, displayType), this.collapse
            )
            return this.contentSize
        }

        override fun render(
            target: T,
            style: HologramStyle,
            path: SelectedPath<HologramWidgetComponent<T>>,
            displayType: DisplayType,
            partialTicks: Float
        ) {
            val selectedType = path.forAny(this)
            style.drawGroupOutline(this.visualSize, selectedType)
            style.stack {
                style.moveToGroupDescription()
                this.renderGroupDescription(target, style, path, displayType, partialTicks)
            }

            if (!this.collapse) {
                style.stack {
                    style.moveAfterDrawGroupOutline(this.descriptionSize(target, style, displayType))
                    this.children.forEach { component ->
                        when (component) {
                            is Single<T> -> {
                                style.drawSingleOutline(
                                    component.visualSize, path.forTerminal(component)
                                )
                                style.stack {
                                    style.moveAfterDrawSingleOutline()
                                    component.render(target, style, path, displayType, partialTicks)
                                }
                            }

                            is Group<T> -> {
                                component.render(target, style, path, displayType, partialTicks)
                            }
                        }
                        style.move(0, component.visualSize.height)
                    }
                }
            }
        }

        fun switchCollapse() {
            this.collapse = !this.collapse
        }

        abstract fun descriptionSize(
            target: T,
            style: HologramStyle,
            displayType: DisplayType
        ): Size

        abstract fun renderGroupDescription(
            target: T,
            style: HologramStyle,
            path: SelectedPath<HologramWidgetComponent<T>>,
            displayType: DisplayType,
            partialTicks: Float
        )

        fun traverseRecursively(code: (HologramWidgetComponent<T>) -> Unit) {
            code.invoke(this)
            for (child in this.children) {
                when (child) {
                    is Group<T> -> child.traverseRecursively(code)
                    is Single<T> -> code.invoke(child)
                }
            }
        }
    }
}
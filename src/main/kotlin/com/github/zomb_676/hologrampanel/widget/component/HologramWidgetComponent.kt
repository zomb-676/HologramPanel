package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.api.IServerDataRequester
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.SelectPathType
import com.github.zomb_676.hologrampanel.util.SelectedPath
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import kotlin.math.max

sealed interface HologramWidgetComponent<T : Any> {
    val contentSize: Size

    /**
     * set by [Group.measureSize]
     */
    val visualSize: Size

    fun render(
        hologramStyle: HologramStyle, selectedPath: SelectedPath<HologramWidgetComponent<T>>, partialTicks: Float
    )

    /**
     * @return [contentSize]
     */
    fun measureSize(target: T, displayType: HologramWidget.DisplayType, hologramStyle: HologramStyle): Size

    abstract class Single<T : Any, R : Any> : HologramWidgetComponent<T> {
        final override var contentSize: Size = Size.ZERO
            internal set
        final override var visualSize: Size = contentSize
            internal set

        final override fun measureSize(
            target: T, displayType: HologramWidget.DisplayType, hologramStyle: HologramStyle
        ): Size = measureContentSize(extract(target), displayType, hologramStyle)

        abstract fun extract(source: T): R

        abstract fun measureContentSize(
            target: R, displayType: HologramWidget.DisplayType, hologramStyle: HologramStyle
        ): Size
    }

    abstract class Group<T : Any>(val children: List<HologramWidgetComponent<T>>) : HologramWidgetComponent<T> {
        final override var contentSize: Size = Size.ZERO
            private set
        final override var visualSize: Size = contentSize
            private set

        var collapse: Boolean = false

        init {
            require(children.isNotEmpty())
        }

        final override fun measureSize(
            target: T, displayType: HologramWidget.DisplayType, hologramStyle: HologramStyle
        ): Size {
            var width = 0
            var height = 0
            if (!this.collapse) {
                for (child in children) {
                    when (child) {
                        is Single<T, *> -> {
                            child.contentSize = child.measureSize(target, displayType, hologramStyle)
                            child.visualSize = hologramStyle.mergeOutlineSizeForSingle(child.contentSize)
                        }

                        is Group<T> -> {
                            child.measureSize(target, displayType, hologramStyle)
                        }
                    }
                    val childSize = child.visualSize
                    width = max(width, childSize.width)
                    height += childSize.height
                }
            }
            this.contentSize = Size.of(width, height)
            this.visualSize = hologramStyle.mergeOutlineSizeForGroup(
                this.contentSize, this.descriptionSize(hologramStyle), this.collapse
            )
            return this.contentSize
        }

        final override fun render(
            hologramStyle: HologramStyle, selectedPath: SelectedPath<HologramWidgetComponent<T>>, partialTicks: Float
        ) {
            val selectedType = selectedPath.forAny(this)
            hologramStyle.drawGroupOutline(this.visualSize, selectedType)
            hologramStyle.stack {
                hologramStyle.moveToGroupDescription()
                this.renderGroupDescription(hologramStyle, selectedType)
            }

            if (!this.collapse) {
                hologramStyle.stack {
                    hologramStyle.moveAfterDrawGroupOutline(this.descriptionSize(hologramStyle))
                    this.children.forEach { component ->
                        when (component) {
                            is Single<T, *> -> {
                                hologramStyle.drawSingleOutline(
                                    component.visualSize, selectedPath.forTerminal(component)
                                )
                                hologramStyle.stack {
                                    hologramStyle.moveAfterDrawSingleOutline()
                                    component.render(hologramStyle, selectedPath, partialTicks)
                                }
                            }

                            is Group<T> -> {
                                component.render(hologramStyle, selectedPath, partialTicks)
                            }
                        }
                        hologramStyle.move(0, component.visualSize.height)
                    }
                }
            }
        }

        fun switchCollapse() {
            this.collapse = !this.collapse
        }

        abstract fun descriptionSize(hologramStyle: HologramStyle): Size

        abstract fun renderGroupDescription(hologramStyle: HologramStyle, selectedType: SelectPathType)

        fun traverseRecursively(code: (HologramWidgetComponent<T>) -> Unit) {
            code.invoke(this)
            for (child in this.children) {
                when (child) {
                    is Group<T> -> child.traverseRecursively(code)
                    is Single<T, *> -> code.invoke(child)
                }
            }
        }

        fun isRequestServerData(): Boolean {
            fun check(group: Group<T>): Boolean {
                if (group is IServerDataRequester<*>) return true
                for (child in group.children) {
                    when (child) {
                        is Single<T, *> -> {
                            if (child is IServerDataRequester<*>) {
                                return true
                            }
                        }
                        is Group<T> -> check(child)
                    }
                }
                return false
            }

            return check(this)
        }
    }
}
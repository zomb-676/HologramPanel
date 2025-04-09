package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.widget.DisplayType
import kotlin.math.max

/**
 * the basic interface for all Hologram component
 */
interface HologramWidgetComponent<T : Any> {

    /**
     * the size for content, not take padding, margin into consideration
     */
    val contentSize: Size

    /**
     * set by [Group.measureSize], bigger than [contentSize], take padding and margin into consideration or layout
     */
    val visualSize: Size

    /**
     * @return [contentSize]
     */
    fun measureSize(target: T, style: HologramStyle, displayType: DisplayType): Size

    /**
     * do actual render, implementation should move [com.mojang.blaze3d.vertex.PoseStack]
     * at make left-top as coordinate as origin point (0,0)
     *
     * area next is [contentSize], should have already taken layout into consideration
     *
     * the implementation of the method should only do [contentSize] render
     */
    fun render(target: T, style: HologramStyle, displayType: DisplayType, partialTicks: Float)

    fun isGroup(): Boolean

    abstract class Single<T : Any> : HologramWidgetComponent<T> {
        final override var contentSize: Size = Size.ZERO
            internal set
        final override var visualSize: Size = contentSize
            internal set

        final override fun isGroup(): Boolean = false
    }

    /**
     * the group has description area
     *
     * @property collapse if the widget is collapsed or not
     */
    abstract class Group<T : Any>(
        val isGlobal: Boolean,
        open val children: List<HologramWidgetComponent<T>>,
        open var collapse: Boolean = false
    ) :
        HologramWidgetComponent<T> {
        final override var contentSize: Size = Size.ZERO
            private set
        final override var visualSize: Size = contentSize
            private set

        final override fun isGroup(): Boolean = true

        /**
         * recursively measure size and addon padding by [HologramStyle.elementPadding]
         * and cache the measured size
         */
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
                height += (this.children.size + 1) * style.elementPadding()
            }
            this.contentSize = Size.of(width, height)
            this.visualSize = style.mergeOutlineSizeForGroup(
                this.contentSize, this.descriptionSize(target, style, displayType), this.collapse
            )
            return this.contentSize
        }

        /**
         * recursively render
         */
        override fun render(
            target: T,
            style: HologramStyle,
            displayType: DisplayType,
            partialTicks: Float
        ) {
            if (style.checkMouseInSize(this.visualSize)) {
                HologramManager.setCollapseTarget(this)
            }
            val descriptionSize = this.descriptionSize(target, style, displayType)
            if (style.checkMouseInSize(this.visualSize)) {
                style.stack {
                    style.pose().translate(0f, 0f, 100f)
                    style.outlineSelected(this.visualSize)
                }
            }
            style.drawGroupOutline(this.isGlobal, this.visualSize, descriptionSize, this.collapse)
            style.stack {
                style.moveToGroupDescription(descriptionSize)
                this.renderGroupDescription(target, style, displayType, partialTicks)
            }

            if (!this.collapse) {
                style.stack {
                    style.moveAfterDrawGroupOutline(descriptionSize)
                    this.children.forEach { component ->
                        when (component) {
                            is Single<T> -> {
                                style.drawSingleOutline(component.visualSize)
                                style.stack {
                                    style.moveAfterDrawSingleOutline()
                                    component.render(target, style, displayType, partialTicks)
                                }
                            }

                            is Group<T> -> {
                                component.render(target, style, displayType, partialTicks)
                            }
                        }
                        style.move(0, component.visualSize.height + style.elementPadding())
                    }
                }
            }
        }

        fun switchCollapse() {
            this.collapse = !this.collapse
        }

        abstract fun descriptionSize(target: T, style: HologramStyle, displayType: DisplayType): Size

        abstract fun renderGroupDescription(target: T, style: HologramStyle, displayType: DisplayType, partialTicks: Float)
    }
}
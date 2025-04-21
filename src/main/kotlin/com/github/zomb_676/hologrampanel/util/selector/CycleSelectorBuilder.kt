package com.github.zomb_676.hologrampanel.util.selector

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.element.EmptyElement
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement

/**
 * the builder for [CycleSelector], use [invoke]
 */
class CycleSelectorBuilder {
    class GroupEntryBuilder {
        val children: MutableList<CycleEntry> = mutableListOf()

        fun add(element: IRenderElement, clickOnClose: Boolean = true, onClick: () -> Unit) {
            val instance = object : CycleEntry.Single {
                override fun onClick(callback: CycleEntry.SelectorCallback) = onClick.invoke()

                override fun onClose() {
                    if (clickOnClose) {
                        onClick()
                    }
                }

                override fun renderContent(style: HologramStyle, partialTick: Float, isHover: Boolean) {
                    element.render(style, partialTick)
                }

                override fun size(style: HologramStyle): Size {
                    element.contentSize = element.measureContentSize(style)
                    return element.contentSize
                }
            }
            children.add(instance)
        }

        inline fun addGroup(element: IRenderElement, code: GroupEntryBuilder.() -> Unit) {
            val groupBuilder = GroupEntryBuilder()
            code.invoke(groupBuilder)
            this.children.add(groupBuilder.build(element))
        }

        fun build(element: IRenderElement): CycleEntry.Group {
            return object : CycleEntry.Group {
                override fun children(): List<CycleEntry> = children
                override fun onClick(callback: CycleEntry.SelectorCallback) {
                    callback.openGroup(this)
                }

                override fun renderContent(style: HologramStyle, partialTick: Float, isHover: Boolean) {
                    element.render(style, partialTick)
                }

                override fun size(style: HologramStyle): Size {
                    element.contentSize = element.measureContentSize(style)
                    return element.contentSize
                }
            }
        }
    }

    companion object {
        inline operator fun invoke(code: GroupEntryBuilder.() -> Unit): CycleSelector {
            val builder = GroupEntryBuilder()
            code.invoke(builder)
            return CycleSelector(builder.build(EmptyElement))
        }
    }
}
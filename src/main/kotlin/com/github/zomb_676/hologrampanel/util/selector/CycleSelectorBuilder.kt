package com.github.zomb_676.hologrampanel.util.selector

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.addClientMessage
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.selector.CycleSelectorBuilder.Companion.invoke
import com.github.zomb_676.hologrampanel.util.switchAndSave
import com.github.zomb_676.hologrampanel.widget.element.ComponentRenderElement
import com.github.zomb_676.hologrampanel.widget.element.EmptyElement
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement
import net.minecraftforge.common.ForgeConfigSpec
import org.jetbrains.annotations.ApiStatus

/**
 * the builder for [CycleSelector], use [invoke]
 */
class CycleSelectorBuilder {
    class SingleEntryBuilder {
        private var frozen = false
        private var renderElement: () -> IRenderElement = { EmptyElement }
        private var onClick: () -> Unit = {}
        private var clickOnClose = true
        private var visible: () -> Boolean = { true }
        private var tickFunction: () -> Unit = {}

        private inline fun onNotFrozen(handler: () -> Unit) {
            if (!frozen) {
                handler()
            }
        }

        fun renderElement(element: IRenderElement) = onNotFrozen {
            renderElement { element }
        }

        fun renderElement(element: () -> IRenderElement) = onNotFrozen {
            this.renderElement = element
        }

        fun onClick(onClick: () -> Unit) = onNotFrozen {
            this.onClick = onClick
        }

        fun notClickOnClose() = onNotFrozen {
            this.clickOnClose = false
        }

        fun visible(visible: () -> Boolean) = onNotFrozen {
            this.visible = visible
        }

        fun tick(code: () -> Unit) = onNotFrozen {
            this.tickFunction = code
        }

        @ApiStatus.Internal
        internal fun build(): CycleEntry.Single {
            this.frozen = true
            return object : CycleEntry.Single {
                private var clicked = false
                private var element: IRenderElement = EmptyElement

                override fun isVisible(): Boolean = visible()

                override fun tick() {
                    this.element = renderElement()
                    tickFunction.invoke()
                }

                override fun onClick(callback: CycleEntry.SelectorCallback, trigType: CycleEntry.TrigType) {
                    onClick.invoke()
                    clicked = true
                }

                override fun onClose(callback: CycleEntry.SelectorCallback) {
                    if (clickOnClose && !clicked) {
                        this.onClick(callback, CycleEntry.TrigType.BY_CLICK)
                    }
                }

                override fun renderContent(style: HologramStyle, partialTick: Float, isHover: Boolean) {
                    element.render(style, partialTick)
                }

                override fun size(style: HologramStyle): Size {
                    element.contentSize = element.measureContentSize(style)
                    return element.contentSize
                }

                override fun scale(): Double = element.getScale()

                override fun toString(): String = "Single(element:$element, clicked:$clicked)"
            }
        }
    }

    class GroupEntryBuilder {
        val children: MutableList<CycleEntry> = mutableListOf()
        private var self: CycleEntry? = null

        fun add(code: SingleEntryBuilder.() -> Unit) {
            val builder = SingleEntryBuilder()
            code.invoke(builder)
            children.add(builder.build())
        }

        fun add(element: IRenderElement, onClick: () -> Unit) {
            add {
                renderElement(element)
                onClick(onClick)
            }
        }

        fun adjustGroup(code: (SingleEntryBuilder).() -> Unit) {
            val builder = SingleEntryBuilder()
            code.invoke(builder)
            this.self = builder.build()
        }

        /**
         * not call [adjustGroup], otherwise, behavior is undefined
         */
        fun addGroup(element: IRenderElement, code: GroupEntryBuilder.() -> Unit) {
            val groupBuilder = GroupEntryBuilder()
            code.invoke(groupBuilder)
            groupBuilder.adjustGroup {
                renderElement { element }
            }
            this.children.add(groupBuilder.build())
        }

        /**
         * must call [adjustGroup]
         */
        fun addGroup(code: GroupEntryBuilder.() -> Unit) {
            val groupBuilder = GroupEntryBuilder()
            code.invoke(groupBuilder)
            this.children.add(groupBuilder.build())
        }

        @ApiStatus.Internal
        internal fun build(): CycleEntry.Group {
            if (self == null) {
                adjustGroup {}
            }
            return object : CycleEntry.Group, CycleEntry.Single by self {
                override fun children(): List<CycleEntry> = children
                override fun onClick(callback: CycleEntry.SelectorCallback, trigType: CycleEntry.TrigType) {
                    callback.openGroup(this)
                }

                override fun toString(): String = "group(childCount:${childrenCount()})"
            }
        }

        fun addOption(value: ForgeConfigSpec.BooleanValue, desc: String) {
            add {
                notClickOnClose()
                var state: Boolean = false
                tick {
                    state = value.get()
                }
                renderElement {
                    if (state) {
                        ComponentRenderElement(desc, 0xffffffff.toInt()).setScale(0.6)
                    } else {
                        ComponentRenderElement(desc, 0xff000000.toInt()).setScale(0.6)
                    }
                }
                onClick {
                    value.switchAndSave()
                    addClientMessage("switch $desc to ${value.get()}")
                }
            }
        }
    }

    companion object {
        operator fun invoke(code: GroupEntryBuilder.() -> Unit): CycleSelector {
            val builder = GroupEntryBuilder()
            code.invoke(builder)
            return CycleSelector(builder.build())
        }
    }
}
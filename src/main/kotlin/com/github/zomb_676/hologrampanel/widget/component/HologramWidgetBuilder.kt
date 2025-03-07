package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.ScreenPosition
import com.github.zomb_676.hologrampanel.util.SelectedPath
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.stackIf
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.math.max

class HologramWidgetBuilder<T : HologramContext>(val context: T) {
    private val stack: Stack<MutableList<HologramWidgetComponent<T>>> = Stack()
    private val helper = this.Helper()
    private var currentInSingle = false
    internal var currentProvider : ComponentProvider<T>? = null

    interface ProviderRelated<T :HologramContext> {
        val provider : ComponentProvider<T>?
    }

    init {
        stack.add(mutableListOf())
    }

    fun add(component: HologramWidgetComponent<T>) {
        requireNotNull(currentProvider)
        require(!currentInSingle)
        stack.peek().add(component)
    }

    fun single(codeBlock: Helper.() -> Unit) {
        requireNotNull(currentProvider)
        require(!currentInSingle) { "not call single in single" }
        currentInSingle = true
        helper.begin()
        codeBlock.invoke(helper)
        stack.peek().add(createSingleFromElements(helper.end()))
        currentInSingle = false
    }

    private fun createSingleFromElements(elements: List<IRenderElement>): HologramWidgetComponent.Single<T> {
        return object : HologramWidgetComponent.Single<T>() , ProviderRelated<T> {
            val elements = elements
            var baseY: Int = 0
            val padding = 1
            override fun measureSize(
                target: T,
                style: HologramStyle,
                displayType: DisplayType
            ): Size {
                this.elements.forEach { it.update() }
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
                    style.stackIf(element.getScale() != 0.0,{style.scale(element.getScale())}) {
                        element.render(style, partialTicks, pathType)
                    }
                    val size = element.contentSize
                    style.move(size.width + padding, -offset.y)
                }
            }

            override val provider: ComponentProvider<T>? = this@HologramWidgetBuilder.currentProvider
        }
    }

    fun group(des: String, codeBlock: () -> Unit) {
        group({ text(des) }, codeBlock)
    }

    fun group(description: Helper.() -> Unit, codeBlock: () -> Unit) {
        requireNotNull(currentProvider)
        require(!currentInSingle) { "not call group in single" }
        stack.push(mutableListOf())
        codeBlock.invoke()
        require(stack.peek().isNotEmpty()) { "group contains nothing added" }
        val desWidget = createSingleFromElements(helper.isolateScope { description.invoke(helper) })
        val group = createGroupForElements(stack.pop().toList(), desWidget)
        stack.peek().add(group)
    }

    private fun createGroupForElements(
        child: List<HologramWidgetComponent<T>>,
        desWidget: HologramWidgetComponent.Single<T>
    ): HologramWidgetComponent.Group<T> {
        return object : HologramWidgetComponent.Group<T>(child) , ProviderRelated<T>{
            val delegate = desWidget
            override fun descriptionSize(
                target: T,
                style: HologramStyle,
                displayType: DisplayType
            ): Size {
                return delegate.measureSize(target, style, displayType)
            }

            override fun renderGroupDescription(
                target: T,
                style: HologramStyle,
                path: SelectedPath<HologramWidgetComponent<T>>,
                displayType: DisplayType,
                partialTicks: Float
            ) {
                return delegate.render(target, style, path, displayType, partialTicks)
            }

            override val provider: ComponentProvider<T>? = this@HologramWidgetBuilder.currentProvider
        }
    }

    internal fun build(description: Helper.() -> Unit): HologramComponentWidget<T> {
        helper.begin()
        description.invoke(helper)
        val descSingle = createSingleFromElements(helper.end())
        val globalGroup = createGroupForElements(stack.pop(), descSingle)
        require(stack.isEmpty())
        return object : HologramComponentWidget<T>(context) {
            override fun initialComponent(): HologramWidgetComponent.Group<T> {
                return globalGroup
            }
        }
    }

    inner class Helper() {
        internal fun isolateScope(code: () -> Unit): List<IRenderElement> {
            this.suppress = true
            require(isolateElements.isEmpty())
            code.invoke()
            require(isolateElements.isNotEmpty())
            this.suppress = false
            val res = isolateElements.toList()
            isolateElements.clear()
            return res
        }

        private var suppress: Boolean = false
        private val isolateElements: MutableList<IRenderElement> = mutableListOf()
        private val elements: MutableList<IRenderElement> = mutableListOf()

        internal fun begin() {
            require(elements.isEmpty())
        }

        internal fun end(): List<IRenderElement> {
            require(elements.isNotEmpty())
            val res = elements.toList()
            this.elements.clear()
            return res
        }

        private fun <T : IRenderElement> T.attach(isStatic : Boolean = false): T {
            val add = if (isStatic) {
                IRenderElement.StaticRenderElement.tryWrap(this)
            } else {
                this
            }
            if (!suppress) {
                elements.add(add)
            } else {
                isolateElements.add(add)
            }
            return this
        }

        fun item(code: () -> Item): IRenderElement.ItemStackElement {
            return IRenderElement.ItemStackElement(false) { ItemStack(code) }.attach()
        }

        fun item(item: Item): IRenderElement.ItemStackElement {
            return IRenderElement.ItemStackElement(false) { ItemStack(item) }.attach(true)
        }

        /**
         * this will call [net.minecraft.client.gui.GuiGraphics.renderItemDecorations]
         */
        fun itemStack(itemStack: () -> ItemStack): IRenderElement.ItemStackElement {
            return IRenderElement.ItemStackElement(true, itemStack).attach()
        }

        /**
         * this will call [net.minecraft.client.gui.GuiGraphics.renderItemDecorations]
         */
        fun itemStack(itemStack: ItemStack): IRenderElement.ItemStackElement {
            return IRenderElement.ItemStackElement(true) { itemStack }.attach(true)
        }

        fun fluid() {

        }

        fun energy() {

        }

        fun progress() {

        }

        fun sprite(sprite: () -> TextureAtlasSprite): IRenderElement {
            return IRenderElement.TextureAtlasSpriteRenderElement(sprite).attach()
        }

        fun sprite(sprite: TextureAtlasSprite): IRenderElement {
            return IRenderElement.TextureAtlasSpriteRenderElement { sprite }.attach(true)
        }

        fun text(str: () -> String): IRenderElement {
            return IRenderElement.StringRenderElement { Component.literal(str.invoke()) }.attach()
        }

        fun text(str: String): IRenderElement {
            return IRenderElement.StringRenderElement { Component.literal(str) }.attach(true)
        }

        fun component(str: () -> Component): IRenderElement {
            return IRenderElement.StringRenderElement { str.invoke() }.attach()
        }

        fun component(str: Component): IRenderElement {
            return IRenderElement.StringRenderElement { str }.attach(true)
        }
    }
}
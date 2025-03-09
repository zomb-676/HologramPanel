package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidget
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.*

class HologramWidgetBuilder<T : HologramContext>(val context: T) {
    private val stack: Stack<MutableList<DynamicBuildComponentWidget<T>>> = Stack()
    private val helper = this.Helper()
    private var currentInSingle = false
    internal var currentProvider: ComponentProvider<T>? = null

    interface ProviderRelated<T : HologramContext> {
        val provider: ComponentProvider<T>?
    }

    init {
        stack.add(mutableListOf())
    }

    fun add(component: DynamicBuildComponentWidget<T>) {
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

    private fun createSingleFromElements(elements: List<IRenderElement>): DynamicBuildComponentWidget.Single<T> {
        return DynamicBuildComponentWidget.Single(currentProvider!!, elements)
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
        val group = createGroupForElements(stack.pop(), desWidget)
        stack.peek().add(group)
    }

    private fun createGroupForElements(
        child: MutableList<DynamicBuildComponentWidget<T>>,
        desWidget: DynamicBuildComponentWidget.Single<T>
    ): DynamicBuildComponentWidget.Group<T> {
        return DynamicBuildComponentWidget.Group(this.currentProvider!!, desWidget, child)
    }

    internal fun build(description: Helper.() -> Unit): HologramComponentWidget<T> {
        helper.begin()
        description.invoke(helper)
        val descSingle = createSingleFromElements(helper.end())
        val globalGroup = createGroupForElements(stack.pop(), descSingle)
        require(stack.isEmpty())
        return DynamicBuildWidget<T>(context, globalGroup)
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

        private fun <T : IRenderElement> T.attach(): T {
            if (!suppress) {
                elements
            } else {
                isolateElements
            }.add(this)
            return this
        }

        fun item(item: Item): IRenderElement.ItemStackElement {
            return IRenderElement.ItemStackElement(false, ItemStack(item)).attach()
        }

        /**
         * this will call [net.minecraft.client.gui.GuiGraphics.renderItemDecorations]
         */
        fun itemStack(itemStack: ItemStack): IRenderElement.ItemStackElement {
            return IRenderElement.ItemStackElement(true, itemStack).attach()
        }

        fun fluid() {

        }

        fun energy() {

        }

        fun progress() {

        }

        fun sprite(sprite: TextureAtlasSprite): IRenderElement {
            return IRenderElement.TextureAtlasSpriteRenderElement(sprite).attach()
        }

        fun text(str: String): IRenderElement {
            return IRenderElement.StringRenderElement(Component.literal(str)).attach()
        }

        fun component(str: Component): IRenderElement {
            return IRenderElement.StringRenderElement(str).attach()
        }
    }
}
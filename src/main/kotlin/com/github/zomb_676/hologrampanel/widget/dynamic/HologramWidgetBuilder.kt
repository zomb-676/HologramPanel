package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.DisplayType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidType
import java.util.*

class HologramWidgetBuilder<T : HologramContext>(val context: T) {
    private val stack: Stack<MutableList<DynamicBuildComponentWidget<T>>> = Stack()
    private val helper = this.Helper()
    private var currentInSingle = false
    internal var currentProvider: ComponentProvider<T, *>? = null

    init {
        stack.add(mutableListOf())
    }

    fun add(component: DynamicBuildComponentWidget<T>) {
        requireNotNull(currentProvider)
        require(!currentInSingle)
        stack.peek().add(component)
    }

    fun single(identityName: String, codeBlock: Helper.() -> Unit) {
        requireNotNull(currentProvider)
        require(!currentInSingle) { "not call single in single" }
        currentInSingle = true
        helper.begin()
        codeBlock.invoke(helper)
        val single = createSingleFromElements(helper.end(), identityName)
        if (single != null) {
            stack.peek().add(single)
        }
        currentInSingle = false
    }

    internal inline fun rebuildScope(
        provider: ComponentProvider<T, *>,
        code: () -> Unit
    ): List<DynamicBuildComponentWidget<T>> {
        this.stack.push(mutableListOf())
        this.currentProvider = provider
        code.invoke()
        this.currentProvider = null
        return this.stack.pop()!!
    }

    private fun createSingleFromElements(
        elements: List<IRenderElement>,
        identityName: String
    ): DynamicBuildComponentWidget.Single<T>? {
        if (elements.isEmpty()) return null
        return DynamicBuildComponentWidget.Single(currentProvider!!, elements, identityName)
    }

    fun group(identityName: String, des: String, codeBlock: () -> Unit) {
        group(identityName, { text(des) }, codeBlock)
    }

    fun group(identityName: String, description: Helper.() -> Unit, codeBlock: () -> Unit) {
        requireNotNull(currentProvider)
        require(!currentInSingle) { "not call group in single" }
        stack.push(mutableListOf())
        codeBlock.invoke()
        val desWidget =
            createSingleFromElements(helper.isolateScope { description.invoke(helper) }, "description_$identityName")!!
        val group = createGroupForElements(stack.pop(), desWidget, false, identityName, false)
        if (group != null) {
            stack.peek().add(group)
        }
    }

    fun lazyGroup(identityName: String, description: String, codeBlock: () -> Unit) {
        lazyGroup(identityName, { text(description) }, codeBlock)
    }

    fun lazyGroup(identityName: String, description: Helper.() -> Unit, codeBlock: () -> Unit) {
        val currentProvider = this.currentProvider!!
        require(!currentInSingle) { "not call group in single" }
        val desWidget =
            createSingleFromElements(helper.isolateScope { description.invoke(helper) }, "description_$identityName")!!
        val group = DynamicBuildComponentWidget.LazyGroup(currentProvider, desWidget, identityName) {
            rebuildScope(currentProvider) {
                codeBlock.invoke()
            }
        }
        stack.peek().add(group)
    }

    private fun createGroupForElements(
        child: MutableList<DynamicBuildComponentWidget<T>>,
        desWidget: DynamicBuildComponentWidget.Single<T>,
        collapse: Boolean,
        identityName: String,
        isGlobal: Boolean
    ): DynamicBuildComponentWidget.Group<T>? {
        if (child.isEmpty()) return null
        return DynamicBuildComponentWidget.Group(isGlobal, this.currentProvider!!, desWidget, child, identityName, collapse)
    }

    /**
     * this provider can only and must produce exactly one single
     */
    internal fun build(
        provider: ComponentProvider<T, *>,
        displayType: DisplayType,
        providers: List<ComponentProvider<T, *>>
    ): DynamicBuildWidget<T> {
        val currentCount = this.stack.peek().size
        helper.begin()
        this.currentProvider = provider
        context.getRememberDataUnsafe<T>().providerScope(provider) {
            provider.appendComponent(this, displayType)
        }
        val currentStack = stack.pop()
        require(currentCount + 1 == currentStack.size) { "can only produce on single" }

        val desc =
            currentStack.removeLast().unsafeCast<DynamicBuildComponentWidget.Single<T>>("must be single not group")
        if (currentStack.isEmpty()) {
            if (providers.isEmpty()) {
                currentStack.add(DynamicBuildComponentWidget.onNoApplicableProvider(context))
            } else {
                currentStack.add(DynamicBuildComponentWidget.onNoActiveProvider(context))
            }
        }
        if (context.getRememberData().serverDataEntries().isNotEmpty()) {
            currentStack.add(DynamicBuildComponentWidget.requireServerData(context))
        }
        val global = createGroupForElements(currentStack, desc, false, "global", true)!!
        require(stack.isEmpty())
        this.currentProvider = null
        return DynamicBuildWidget(context, global, providers)
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
            val res = elements.toList()
            this.elements.clear()
            return res
        }

        @PublishedApi
        internal fun <T : IRenderElement> T.attach(): T {
            if (!suppress) {
                elements
            } else {
                isolateElements
            }.add(this)
            return this
        }

        inline fun <T : IRenderElement> renderable(code: () -> T): T {
            return code.invoke().attach()
        }

        fun item(item: Item): IRenderElement.ItemStackElement {
            return IRenderElement.ItemStackElement(false, ItemStack(item)).attach()
        }

        fun items(items: List<ItemStack>): IRenderElement.ItemsElement {
            return IRenderElement.ItemsElement(items).attach()
        }

        fun itemsInteractive(items: List<ItemStack>, input: Boolean): IRenderElement.InteractiveItemsElement {
            return IRenderElement.InteractiveItemsElement(items).attach()
        }

        /**
         * this will call [net.minecraft.client.gui.GuiGraphics.renderItemDecorations]
         */
        fun itemStack(itemStack: ItemStack): IRenderElement.ItemStackElement {
            return IRenderElement.ItemStackElement(itemStack = itemStack).attach()
        }

        fun itemInteractive(item: ItemStack, interactiveSlot: Int): IRenderElement.InteractiveItemElement {
            require(interactiveSlot >= 0)
            return IRenderElement.InteractiveItemElement(item, interactiveSlot).attach()
        }

        fun workingArrowProgress(progress: ProgressData): IRenderElement.WorkingArrowProgressBarElement {
            val element = IRenderElement.WorkingArrowProgressBarElement(progress)
            return element.attach()
        }

        fun workingCycleProgress(
            progress: ProgressData,
            radius: Float = 10.0f
        ): IRenderElement.WorkingCircleProgressElement {
            val element = IRenderElement.WorkingCircleProgressElement(progress, radius, 0.0f)
            return element.attach()
        }

        fun workingTorusProgress(
            progress: ProgressData,
            outRadius: Float = 10.0f,
            inRadius: Float = 8.0f
        ): IRenderElement.WorkingCircleProgressElement {
            val element = IRenderElement.WorkingCircleProgressElement(progress, outRadius, inRadius)
            return element.attach()
        }

        fun energyBar(progressBar: ProgressData): IRenderElement.EnergyBarElement {
            return IRenderElement.EnergyBarElement(progressBar).attach()
        }

        fun fluid(progressBar: ProgressData, fluid: FluidType): IRenderElement.FluidBarElement {
            return IRenderElement.FluidBarElement(progressBar, fluid).attach()
        }

        fun sprite(sprite: TextureAtlasSprite): IRenderElement.TextureAtlasSpriteRenderElement {
            return IRenderElement.TextureAtlasSpriteRenderElement(sprite).attach()
        }

        fun text(str: String): IRenderElement.StringRenderElement {
            return IRenderElement.StringRenderElement(Component.literal(str)).attach()
        }

        fun component(str: Component): IRenderElement.StringRenderElement {
            return IRenderElement.StringRenderElement(str).attach()
        }

        fun takeSize(size: Size): IRenderElement.EmptySized {
            return IRenderElement.EmptySized(size).attach()
        }

        fun screenTooltip(item: ItemStack): IRenderElement.ScreenTooltipElement {
            return IRenderElement.ScreenTooltipElement(item).attach()
        }

        fun heart(): IRenderElement.TextureAtlasSpriteRenderElement {
            val location = Gui.HeartType.NORMAL.getSprite(false, false, false)
            val atlas = Minecraft.getInstance().guiSprites.getSprite(location)
            return sprite(atlas).apply { setPositionOffset(0, -1) }
        }

        fun entity(entity: Entity) = if (entity is ItemEntity) {
            entity(entity, 15.0).setPositionOffset(0, 5)
        } else {
            entity(entity, 10.0)
        }

        fun entity(entity: Entity, scale: Double): IRenderElement.EntityRenderElement {
            return IRenderElement.EntityRenderElement(entity, scale).attach()
        }

        /**
         * must use the [helper] provided by this lambda
         */
        fun vertical(code: Helper.() -> Unit): IRenderElement.VerticalBox {
            val helper = Helper()
            code.invoke(helper)
            return IRenderElement.VerticalBox(helper.elements, context).attach()
        }
    }
}
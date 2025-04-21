package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.trans.TransHandle
import com.github.zomb_676.hologrampanel.trans.TransSource
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.element.*
import com.github.zomb_676.hologrampanel.widget.element.progress.EnergyBarElement
import com.github.zomb_676.hologrampanel.widget.element.progress.FluidBarElement
import com.github.zomb_676.hologrampanel.widget.element.progress.WorkingArrowProgressBarElement
import com.github.zomb_676.hologrampanel.widget.element.progress.WorkingCircleProgressElement
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableBiMap
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidType
import net.neoforged.neoforge.items.IItemHandler
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
        elements: ImmutableBiMap<IRenderElement, String>,
        identityName: String
    ): DynamicBuildComponentWidget.Single<T>? {
        if (elements.isEmpty()) return null
        return DynamicBuildComponentWidget.Single(currentProvider!!, elements, identityName)
    }

    fun group(identityName: String, des: String, codeBlock: () -> Unit) {
        group(identityName, { text("description_for_group", des) }, codeBlock)
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
        lazyGroup(identityName, { text("description_for_lazy", description) }, codeBlock)
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
        internal fun isolateScope(code: () -> Unit): ImmutableBiMap<IRenderElement, String> {
            this.suppress = true
            require(isolateElements.isEmpty())
            code.invoke()
            require(isolateElements.isNotEmpty())
            this.suppress = false
            val res: ImmutableBiMap<IRenderElement, String> = ImmutableBiMap.copyOf(isolateElements)
            isolateElements.clear()
            return res
        }

        private var suppress: Boolean = false
        private val isolateElements: BiMap<IRenderElement, String> = HashBiMap.create()
        private val elements: BiMap<IRenderElement, String> = HashBiMap.create()
        internal fun begin() {
            require(elements.isEmpty())
        }

        internal fun end(): ImmutableBiMap<IRenderElement, String> {
            val res: ImmutableBiMap<IRenderElement, String> = ImmutableBiMap.copyOf(this.elements)
            this.elements.clear()
            return res
        }

        @PublishedApi
        internal fun <T : IRenderElement> T.attach(name: String): T {
            if (!suppress) {
                elements
            } else {
                isolateElements
            }.put(this, name)
            return this
        }

        inline fun <T : IRenderElement> renderable(name: String, code: () -> T): T {
            return code.invoke().attach(name)
        }

        fun item(name: String, item: Item): ItemStackElement {
            return ItemStackElement(false, ItemStack(item)).attach(name)
        }

        fun items(name: String, items: List<ItemStack>): ItemsElement {
            return ItemsElement(items).attach(name)
        }

        fun <S : Any> itemsInteractive(
            name: String,
            items: List<ItemStack>,
            source: TransSource<S>,
            handle: TransHandle<S, IItemHandler>
        ): InteractiveItemsElement {
            return InteractiveItemsElement.create(items, source, handle, true).attach(name)
        }

        /**
         * this will call [net.minecraft.client.gui.GuiGraphics.renderItemDecorations]
         */
        fun itemStack(name: String, itemStack: ItemStack): ItemStackElement {
            return ItemStackElement(itemStack = itemStack).attach(name)
        }

        fun <S : Any> itemInteractive(
            name: String,
            item: ItemStack,
            interactiveSlot: Int,
            source: TransSource<S>,
            transPath: TransHandle<S, IItemHandler>
        ): InteractiveItemElement {
            require(interactiveSlot >= 0)
            return InteractiveItemElement.create(item, interactiveSlot, source, transPath).attach(name)
        }

        fun workingArrowProgress(name: String, progress: ProgressData): WorkingArrowProgressBarElement {
            val element = WorkingArrowProgressBarElement(progress)
            return element.attach(name)
        }

        fun workingCycleProgress(
            name: String,
            progress: ProgressData,
            radius: Float = 10.0f
        ): WorkingCircleProgressElement {
            val element = WorkingCircleProgressElement(progress, radius, 0.0f)
            return element.attach(name)
        }

        fun workingTorusProgress(
            name: String,
            progress: ProgressData,
            outRadius: Float = 10.0f,
            inRadius: Float = 8.0f
        ): WorkingCircleProgressElement {
            val element = WorkingCircleProgressElement(progress, outRadius, inRadius)
            return element.attach(name)
        }

        fun energyBar(name: String, progressBar: ProgressData): EnergyBarElement {
            return EnergyBarElement(progressBar).attach(name)
        }

        fun fluid(name: String, progressBar: ProgressData, fluid: FluidType): FluidBarElement {
            return FluidBarElement(progressBar, fluid).attach(name)
        }

        fun sprite(name: String, sprite: TextureAtlasSprite): TextureAtlasSpriteRenderElement {
            return TextureAtlasSpriteRenderElement(sprite).attach(name)
        }

        fun text(name: String, str: String): ComponentRenderElement {
            return ComponentRenderElement(Component.literal(str)).attach(name)
        }

        fun component(name: String, str: Component): ComponentRenderElement {
            return ComponentRenderElement(str).attach(name)
        }

        fun screenTooltip(name: String, item: ItemStack): ScreenTooltipElement {
            return ScreenTooltipElement(item).attach(name)
        }

        fun heart(name: String): TextureAtlasSpriteRenderElement {
            val location = Gui.HeartType.NORMAL.getSprite(false, false, false)
            val atlas = Minecraft.getInstance().guiSprites.getSprite(location)
            return sprite(name, atlas).apply { setPositionOffset(0, -1) }
        }

        fun entity(name: String, entity: Entity) = if (entity is ItemEntity) {
            entity(name, entity, 15.0).setPositionOffset(0, 5)
        } else {
            entity(name, entity, 10.0)
        }

        fun entity(name: String, entity: Entity, scale: Double): EntityRenderElement {
            return EntityRenderElement(entity, scale).attach(name)
        }

        /**
         * must use the [helper] provided by this lambda
         */
        fun vertical(name: String, code: Helper.() -> Unit): VerticalBox {
            val helper = Helper()
            code.invoke(helper)
            return VerticalBox(ImmutableBiMap.copyOf(helper.elements), context).attach(name)
        }
    }
}
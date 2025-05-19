package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.DebugHelper
import com.github.zomb_676.hologrampanel.addon.BuildInPlugin.Companion.DefaultBlockDescriptionProvider
import com.github.zomb_676.hologrampanel.addon.BuildInPlugin.Companion.DefaultEntityDescriptionProvider
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.*
import com.github.zomb_676.hologrampanel.util.packed.AlignedScreenPosition
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent.Group
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent.Single
import com.github.zomb_676.hologrampanel.widget.element.ComponentRenderElement
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement
import com.google.common.collect.ImmutableBiMap
import net.minecraft.network.chat.Component
import kotlin.math.max
import kotlin.math.min

/**
 * the actual usage, not directly use the instance of class across frames
 * as they will be rebuilt when data changes, use [getCurrent] or use property get delegate
 */
sealed interface DynamicBuildComponentWidget<T : HologramContext> : HologramWidgetComponent<T>, RebuildValue<DynamicBuildComponentWidget<T>?> {
    fun getProvider(): ComponentProvider<T, *>
    fun getIdentityName(): String

    /**
     * should be called when the [DynamicBuildWidget] is removed
     */
    fun setNoNewReplace()
    override fun getCurrent(): DynamicBuildComponentWidget<T>?

    /**
     * is made up of [com.github.zomb_676.hologrampanel.widget.element.IRenderElement], layout in a horizontal way
     */
    open class Single<T : HologramContext>(
        private val provider: ComponentProvider<T, *>, val elements: ImmutableBiMap<IRenderElement, String>, private val identityName: String
    ) : HologramWidgetComponent.Single<T>(), DynamicBuildComponentWidget<T> {
        private var baseY: Int = 0
        private val padding = 1
        private var current: Single<T>? = this

        override fun measureSize(target: T, style: HologramStyle, displayType: DisplayType): Size {
            var width = 0
            var height = 0
            var calculatedSizeElement = 0
            this.elements.keys.forEach {
                it.contentSize = it.measureContentSize(style)
                val offset = it.getPositionOffset()
                if (it.hasCalculateSize()) {
                    calculatedSizeElement++
                    val elementHeight = if (it.isLimitHeight()) {
                        min(it.getLimitHeight(), it.contentSize.height)
                    } else it.contentSize.height
                    if (offset == AlignedScreenPosition.ZERO) {
                        width += it.contentSize.width
                        height = max(height, elementHeight)
                    } else {
                        width += it.contentSize.width + offset.x
                        if (offset.y < 0) {
                            baseY = max(baseY, -offset.y)
                        }
                        height = max(height, elementHeight + offset.y)
                    }
                }
            }
            width += (calculatedSizeElement - 1) * padding
            return Size.of(width, height)
        }

        override fun render(target: T, style: HologramStyle, displayType: DisplayType, partialTicks: Float) {
            val inMouse = style.checkMouseInSize(this.visualSize)
            if (baseY != 0) {
                style.move(0, baseY)
            }
            this.elements.keys.forEach { element ->
                val offset = element.getPositionOffset()
                val size = element.contentSize
                style.stack {
                    if (offset != AlignedScreenPosition.ZERO) style.move(offset)
                    if (element.getScale() != 1.0) style.scale(element.getScale())
                    if (element.hasAdditionLayer()) style.translate(0.0, 0.0, element.additionLayer().toDouble())
                    if (element.isLimitHeight(size.height)) {
                        style.guiGraphics.enableScissor(0, 0, size.width, element.getLimitHeight())
                        style.translate(0f, timeInterpolation(size.height - element.getLimitHeight()).toFloat())
                    }
                    if (inMouse && style.checkMouseInSize(size)) {
                        DebugHelper.Client.recordHoverElement(element)
                        if (Config.Client.renderWidgetDebugInfo.get()) {
                            style.stack {
                                style.translate(0f, 0f, 100f)
                                style.outline(size, 0xff0000ff.toInt())
                            }
                        }
                        if (element is HologramInteractive) {
                            HologramManager.submitInteractive(this, element, target, size, style)
                        }
                    }
                    element.render(style, partialTicks)
                    if (element.isLimitHeight(size.height)) style.guiGraphics.disableScissor()
                }
                if (element.hasCalculateSize()) {
                    style.move(size.width + padding + offset.x, 0)
                }
            }
        }

        override fun getProvider(): ComponentProvider<T, *> = provider
        override fun getIdentityName(): String = this.identityName

        override fun getCurrent(): Single<T>? {
            var current: Single<T> = current ?: return null
            while (current != current.current) {
                current = current.current ?: run {
                    this.current = null
                    return null
                }
            }
            this.current = current
            return current
        }

        fun setReplacedBy(newCurrent: Single<T>) {
            this.current = newCurrent
        }

        override fun setNoNewReplace() {
            this.current = null
        }
    }

    companion object {
        private val noActiveProvider = ComponentRenderElement(Component.literal("No Active"))
        private val noApplicableProvider = ComponentRenderElement(Component.literal("No Applicable"))
        private val requireServerDataElement = ComponentRenderElement(Component.literal("Waiting Server Packet"))

        object NoActiveProvider {
            val block: Single<BlockHologramContext> = OrdinarySingle<BlockHologramContext>(
                DefaultBlockDescriptionProvider, noActiveProvider, "no_active_provider"
            )
            val entity: Single<EntityHologramContext> = OrdinarySingle<EntityHologramContext>(
                DefaultEntityDescriptionProvider, noActiveProvider, "no_active)provider"
            )
        }

        object NoApplicableProvider {
            val block: Single<BlockHologramContext> = OrdinarySingle<BlockHologramContext>(
                DefaultBlockDescriptionProvider, noApplicableProvider, "no_applicable_provider"
            )
            val entity: Single<EntityHologramContext> = OrdinarySingle<EntityHologramContext>(
                DefaultEntityDescriptionProvider, noApplicableProvider, "no_applicable_provider"
            )
        }

        object RequireServerData {
            val block: Single<BlockHologramContext> = OrdinarySingle<BlockHologramContext>(
                DefaultBlockDescriptionProvider, requireServerDataElement, "require_server_data"
            )
            val entity: Single<EntityHologramContext> = OrdinarySingle<EntityHologramContext>(
                DefaultEntityDescriptionProvider, requireServerDataElement, "require_server_data"
            )
        }

        fun <T : HologramContext> onNoActiveProvider(context: T): Single<T> = when (context) {
            is BlockHologramContext -> NoActiveProvider.block
            is EntityHologramContext -> NoActiveProvider.entity
        }.unsafeCast()

        fun <T : HologramContext> onNoApplicableProvider(context: T): Single<T> = when (context) {
            is BlockHologramContext -> NoApplicableProvider.block
            is EntityHologramContext -> NoApplicableProvider.entity
        }.unsafeCast()

        fun <T : HologramContext> requireServerData(context: T): Single<T> = when (context) {
            is BlockHologramContext -> RequireServerData.block
            is EntityHologramContext -> RequireServerData.entity
        }.unsafeCast()
    }

    class OrdinarySingle<T : HologramContext>(
        provider: ComponentProvider<T, *>, element: IRenderElement, identityName: String
    ) : Single<T>(provider, ImmutableBiMap.of(element, identityName), identityName)

    /**
     * the group has an addition [Single] for description usages
     */
    open class Group<T : HologramContext>(
        isGlobal: Boolean,
        private val provider: ComponentProvider<T, *>,
        val descriptionWidget: Single<T>,
        override var children: List<DynamicBuildComponentWidget<T>>,
        private val identityName: String,
        collapse: Boolean
    ) : HologramWidgetComponent.Group<T>(isGlobal, children, collapse), DynamicBuildComponentWidget<T> {

        private var current: Group<T>? = this

        override fun descriptionSize(
            target: T, style: HologramStyle, displayType: DisplayType
        ): Size = descriptionWidget.measureSize(target, style, displayType)

        override fun renderGroupDescription(
            target: T, style: HologramStyle, displayType: DisplayType, partialTicks: Float
        ) {
            descriptionWidget.render(target, style, displayType, partialTicks)
        }

        override fun getProvider(): ComponentProvider<T, *> = provider
        override fun getIdentityName(): String = identityName

        override fun toString(): String {
            return "Group(name='$identityName', visual:${this.visualSize}, content:${this.contentSize})"
        }

        override fun getCurrent(): Group<T>? {
            var current: Group<T> = current ?: return null
            while (current != current.current) {
                current = current.current ?: run {
                    this.current = null
                    return null
                }
            }
            this.current = current
            return current
        }

        fun setReplacedBy(newCurrent: Group<T>) {
            this.current = newCurrent
        }

        override fun setNoNewReplace() {
            this.current = null
        }
    }

    /**
     * a lazy loaded and collapsed by default variant of [Group]
     */
    class LazyGroup<T : HologramContext>(
        provider: ComponentProvider<T, *>,
        descriptionWidget: Single<T>,
        identityName: String,
        val initializer: () -> List<DynamicBuildComponentWidget<T>>
    ) : Group<T>(false, provider, descriptionWidget, listOf(), identityName, true) {
        override var collapse: Boolean = true
            set(value) {
                field = value
                if (!value) {
                    tryLoadChildren()
                }
            }
        private var actualChildren: List<DynamicBuildComponentWidget<T>> = listOf()

        fun tryLoadChildren() {
            if (actualChildren.isNotEmpty()) return
            actualChildren = initializer.invoke()
        }

        override var children: List<DynamicBuildComponentWidget<T>> = listOf()
            get() = if (actualChildren.isNotEmpty()) {
                actualChildren
            } else {
                field
            }
    }
}
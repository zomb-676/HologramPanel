package com.github.zomb_676.hologrampanel.widget.dynamic

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
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent
import net.minecraft.network.chat.Component
import kotlin.math.max

sealed interface DynamicBuildComponentWidget<T : HologramContext> : HologramWidgetComponent<T> {
    fun getProvider(): ComponentProvider<T, *>
    fun getIdentityName(): String

    open class Single<T : HologramContext>(
        private val provider: ComponentProvider<T, *>,
        val elements: List<IRenderElement>,
        private val identityName: String
    ) : HologramWidgetComponent.Single<T>(), DynamicBuildComponentWidget<T> {
        private var baseY: Int = 0
        private val padding = 1

        override fun measureSize(
            target: T, style: HologramStyle, displayType: DisplayType
        ): Size {
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
            displayType: DisplayType,
            partialTicks: Float
        ) {
            val inMouse = style.checkMouseInSize(this.contentSize)
            if (baseY != 0) {
                style.move(0, baseY)
            }
            this.elements.forEach { element ->
                val offset = element.getPositionOffset()
                if (offset != ScreenPosition.ZERO) {
                    style.move(offset)
                }
                val size = element.contentSize
                style.stackIf(element.getScale() != 1.0, { style.scale(element.getScale()) }) {
                    if (inMouse && style.checkMouseInSize(size)) {
                        DebugHelper.Client.recordHoverElement(element)
                        if (element is HologramInteractive) {
                            HologramManager.submitInteractive(InteractiveEntry.of(element, target,size, style))
                        }
                    }
                    element.render(style, partialTicks)
                }
                style.move(size.width + padding, -offset.y)
            }
        }

        override fun getProvider(): ComponentProvider<T, *> = provider
        override fun getIdentityName(): String = this.identityName
    }

    companion object {
        private val noActiveProvider = IRenderElement.StringRenderElement(Component.literal("No Active"))
        private val noApplicableProvider = IRenderElement.StringRenderElement(Component.literal("No Applicable"))
        private val requireServerDataElement =
            IRenderElement.StringRenderElement(Component.literal("Waiting Server Packet"))

        object NoActiveProvider {
            val block: Single<BlockHologramContext> =
                OrdinarySingle<BlockHologramContext>(
                    DefaultBlockDescriptionProvider,
                    noActiveProvider,
                    "no_active_provider"
                )
            val entity: Single<EntityHologramContext> =
                OrdinarySingle<EntityHologramContext>(
                    DefaultEntityDescriptionProvider,
                    noActiveProvider,
                    "no_active)provider"
                )
        }

        object NoApplicableProvider {
            val block: Single<BlockHologramContext> =
                OrdinarySingle<BlockHologramContext>(
                    DefaultBlockDescriptionProvider,
                    noApplicableProvider,
                    "no_applicable_provider"
                )
            val entity: Single<EntityHologramContext> =
                OrdinarySingle<EntityHologramContext>(
                    DefaultEntityDescriptionProvider,
                    noApplicableProvider,
                    "no_applicable_provider"
                )
        }

        object RequireServerData {
            val block: Single<BlockHologramContext> =
                OrdinarySingle<BlockHologramContext>(
                    DefaultBlockDescriptionProvider,
                    requireServerDataElement,
                    "require_server_data"
                )
            val entity: Single<EntityHologramContext> =
                OrdinarySingle<EntityHologramContext>(
                    DefaultEntityDescriptionProvider,
                    requireServerDataElement,
                    "require_server_data"
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
    ) : Single<T>(provider, listOf(element), identityName)

    open class Group<T : HologramContext>(
        private val provider: ComponentProvider<T, *>,
        val descriptionWidget: Single<T>,
        override var children: List<DynamicBuildComponentWidget<T>>,
        private val identityName: String,
        collapse: Boolean
    ) : HologramWidgetComponent.Group<T>(children, collapse), DynamicBuildComponentWidget<T> {
        override fun descriptionSize(
            target: T, style: HologramStyle, displayType: DisplayType
        ): Size = descriptionWidget.measureSize(target, style, displayType)

        override fun renderGroupDescription(
            target: T,
            style: HologramStyle,
            displayType: DisplayType,
            partialTicks: Float
        ) {
            descriptionWidget.render(target, style, displayType, partialTicks)
        }

        override fun getProvider(): ComponentProvider<T, *> = provider
        override fun getIdentityName(): String = identityName
    }

    class LazyGroup<T : HologramContext>(
        provider: ComponentProvider<T, *>,
        descriptionWidget: Single<T>,
        identityName: String,
        val initializer: () -> List<DynamicBuildComponentWidget<T>>
    ) : Group<T>(provider, descriptionWidget, listOf(), identityName, true) {
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

        override fun descriptionSize(
            target: T, style: HologramStyle, displayType: DisplayType
        ): Size {
            return super.descriptionSize(target, style, displayType)
        }

        override fun renderGroupDescription(
            target: T,
            style: HologramStyle,
            displayType: DisplayType,
            partialTicks: Float
        ) {
            super.renderGroupDescription(target, style, displayType, partialTicks)
        }

        override fun render(
            target: T,
            style: HologramStyle,
            displayType: DisplayType,
            partialTicks: Float
        ) {
            super.render(target, style, displayType, partialTicks)
        }
    }
}
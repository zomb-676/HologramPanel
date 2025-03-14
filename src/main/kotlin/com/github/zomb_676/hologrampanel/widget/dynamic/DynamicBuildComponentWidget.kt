package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.BuildInPlugin.Companion.DefaultBlockDescriptionProvider
import com.github.zomb_676.hologrampanel.BuildInPlugin.Companion.DefaultEntityDescriptionProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.*
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent
import net.minecraft.network.chat.Component
import kotlin.math.max

sealed interface DynamicBuildComponentWidget<T : HologramContext> : HologramWidgetComponent<T> {
    fun getProvider(): ComponentProvider<T>

    open class Single<T : HologramContext>(
        private val provider: ComponentProvider<T>,
        val elements: List<IRenderElement>
    ) :
        HologramWidgetComponent.Single<T>(), DynamicBuildComponentWidget<T> {
        private var baseY: Int = 0
        private val padding = 1

        override fun measureSize(
            target: T,
            style: HologramStyle,
            displayType: DisplayType
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
                style.stackIf(element.getScale() != 1.0, { style.scale(element.getScale()) }) {
                    element.render(style, partialTicks, pathType)
                }
                val size = element.contentSize
                style.move(size.width + padding, -offset.y)
            }
        }

        override fun getProvider(): ComponentProvider<T> = provider
    }

    companion object {
        private val noActiveElement = IRenderElement.StringRenderElement(Component.literal("No Active Provider Found"))
        private val requireServerDataElement =
            IRenderElement.StringRenderElement(Component.literal("Waiting for Server Packet"))

        object NoProvider {
            val block: Single<BlockHologramContext> =
                SpecialProvider<BlockHologramContext>(DefaultBlockDescriptionProvider, noActiveElement)
            val entity: Single<EntityHologramContext> =
                SpecialProvider<EntityHologramContext>(DefaultEntityDescriptionProvider, noActiveElement)
        }

        object RequireServerData {
            val block: Single<BlockHologramContext> =
                SpecialProvider<BlockHologramContext>(DefaultBlockDescriptionProvider, requireServerDataElement)
            val entity: Single<EntityHologramContext> =
                SpecialProvider<EntityHologramContext>(DefaultEntityDescriptionProvider, requireServerDataElement)
        }

        fun <T : HologramContext> onNoProvider(context: T): Single<T> = when (context) {
            is BlockHologramContext -> NoProvider.block
            is EntityHologramContext -> NoProvider.entity
        }.unsafeCast()

        fun <T : HologramContext> requireServerData(context: T): Single<T> = when (context) {
            is BlockHologramContext -> RequireServerData.block
            is EntityHologramContext -> RequireServerData.entity
        }.unsafeCast()
    }

    private class SpecialProvider<T : HologramContext>(provider: ComponentProvider<T>, element: IRenderElement) :
        Single<T>(provider, listOf(element))

    class Group<T : HologramContext>(
        private val provider: ComponentProvider<T>,
        val descriptionWidget: Single<T>,
        override var children: List<DynamicBuildComponentWidget<T>>
    ) : HologramWidgetComponent.Group<T>(children), DynamicBuildComponentWidget<T> {
        override fun descriptionSize(
            target: T,
            style: HologramStyle,
            displayType: DisplayType
        ): Size = descriptionWidget.measureSize(target, style, displayType)

        override fun renderGroupDescription(
            target: T,
            style: HologramStyle,
            path: SelectedPath<HologramWidgetComponent<T>>,
            displayType: DisplayType,
            partialTicks: Float
        ) {
            descriptionWidget.render(target, style, path, displayType, partialTicks)
        }

        override fun getProvider(): ComponentProvider<T> = provider
    }
}
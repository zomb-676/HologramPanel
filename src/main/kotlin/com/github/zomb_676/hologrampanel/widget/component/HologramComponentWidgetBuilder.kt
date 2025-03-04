package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.SelectPathType
import com.github.zomb_676.hologrampanel.util.SelectedPath
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.entity.BlockEntity

class HologramComponentWidgetBuilder<T : Any> {

    private val initial: MutableList<(T) -> HologramWidgetComponent<T>> = mutableListOf()
    private val updater: MutableList<(HologramComponentWidgetBuilder<T>) -> HologramComponentWidgetBuilder<T>> =
        mutableListOf()

    fun add(
        initial: (T) -> HologramWidgetComponent<T>,
        updater: (HologramComponentWidgetBuilder<T>) -> HologramComponentWidgetBuilder<T>
    ) {
        this.initial.add(initial)
        this.updater.add(updater)
    }

    inline fun <R : Any> add(
        crossinline extractor: (T).() -> R, adapter: TypeAdapter<R>
    ): HologramComponentWidgetBuilder<T> {

        val x = { target: T ->
            object : HologramWidgetComponent.Single<T, R>() {
                override fun extract(source: T): R = extractor.invoke(source)

                override fun measureContentSize(
                    target: R, displayType: HologramWidget.DisplayType, hologramStyle: HologramStyle
                ): Size = adapter.measureContentSize(target, hologramStyle)

                override fun render(
                    hologramStyle: HologramStyle,
                    selectedPath: SelectedPath<HologramWidgetComponent<T>>,
                    partialTicks: Float
                ) = adapter.render(hologramStyle, partialTicks, selectedPath.forTerminal(this))
            }
        }
        add(x) { it }
        return this
    }

    companion object {
        fun <T : Any> builder() = HologramComponentWidgetBuilder<T>()

        val builder = builder<BlockEntity>()
            .add(BlockEntity::getBlockPos, StringAdapter { pos -> "pos:(x:${pos.x},y:${pos.y},z:${pos.z})" })
            .add(BlockEntity::getBlockState, StringAdapter { state -> state.block.toString() })
            .build { b -> BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(b.type).toString() }
    }

    private inline fun build(crossinline description: (T) -> String): (T) -> HologramComponentWidget<T> {
        return { target ->
            object : HologramComponentWidget<T>(target) {
                override fun initialComponent(): HologramWidgetComponent.Group<T> {
                    val g =
                        object : HologramWidgetComponent.Group<T>(initial.map { it.invoke(target) }.toMutableList()) {

                            var desc: String = ""

                            override fun descriptionSize(hologramStyle: HologramStyle): Size {
                                desc = description.invoke(target)
                                return Size.of(hologramStyle.font.width(desc), hologramStyle.font.lineHeight)
                            }

                            override fun renderGroupDescription(
                                hologramStyle: HologramStyle,
                                selectedType: SelectPathType
                            ) {
                                hologramStyle.drawString(desc)
                            }
                        }
                    val child = initial.map { it.invoke(target) }.toMutableList()
                    val g2 = object : HologramWidgetComponent.Group<T>(child.toMutableList().apply { add(g) }) {

                        var desc: String = ""

                        override fun descriptionSize(hologramStyle: HologramStyle): Size {
                            desc = description.invoke(target)
                            return Size.of(hologramStyle.font.width(desc), hologramStyle.font.lineHeight)
                        }

                        override fun renderGroupDescription(
                            hologramStyle: HologramStyle,
                            selectedType: SelectPathType
                        ) {
                            hologramStyle.drawString(desc)
                        }
                    }
                    return g2
                }
            }
        }
    }

    interface TypeAdapter<R> {
        fun measureContentSize(target: R, hologramStyle: HologramStyle): Size
        fun render(hologramStyle: HologramStyle, partialTicks: Float, forTerminal: SelectPathType)
    }

    @FunctionalInterface
    abstract class StringAdapter<R> : TypeAdapter<R> {
        private var display: String = ""
        override fun measureContentSize(target: R, hologramStyle: HologramStyle): Size {
            display = extractString(target)
            return Size.of(hologramStyle.font.width(display), hologramStyle.font.lineHeight)
        }

        override fun render(hologramStyle: HologramStyle, partialTicks: Float, forTerminal: SelectPathType) =
            hologramStyle.drawString(display, 0, 0, DyeColor.BLACK.textColor)

        abstract fun extractString(target: R): String

        companion object {
            inline operator fun <R> invoke(crossinline code: (R) -> String): StringAdapter<R> =
                object : StringAdapter<R>() {
                    override fun extractString(target: R): String = code.invoke(target)
                }
        }
    }
}
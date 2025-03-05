package com.github.zomb_676.hologrampanel.widget.component

import com.github.zomb_676.hologrampanel.api.IContextType
import com.github.zomb_676.hologrampanel.api.IServerDataProcessor
import com.github.zomb_676.hologrampanel.api.IServerDataRequester
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.SelectPathType
import com.github.zomb_676.hologrampanel.util.SelectedPath
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.FurnaceBlockEntity

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
                    child.add(g)
                    val t = object : HologramWidgetComponent.Single<FurnaceBlockEntity, FurnaceBlockEntity>(),
                        IServerDataRequester<FurnaceBlockEntity> {
                        override fun extract(source: FurnaceBlockEntity): FurnaceBlockEntity {
                            return source
                        }

                        var item0: ItemStack = ItemStack.EMPTY
                        var item1: ItemStack = ItemStack.EMPTY
                        var item2: ItemStack = ItemStack.EMPTY

                        override fun measureContentSize(
                            target: FurnaceBlockEntity,
                            displayType: DisplayType,
                            hologramStyle: HologramStyle
                        ): Size {
                            return Size.of(16 * 3 + 4 * 2, 16)
                        }

                        override fun render(
                            hologramStyle: HologramStyle,
                            selectedPath: SelectedPath<HologramWidgetComponent<FurnaceBlockEntity>>,
                            partialTicks: Float
                        ) {
                            val font = Minecraft.getInstance().font
                            hologramStyle.guiGraphics.renderItem(item0, 0, 0)
                            hologramStyle.guiGraphics.renderItemDecorations(font, item0,0,0)
                            hologramStyle.guiGraphics.renderItem(item1, 20, 0)
                            hologramStyle.guiGraphics.renderItemDecorations(font, item1,20,0)
                            hologramStyle.guiGraphics.renderItem(item2, 40, 0)
                            hologramStyle.guiGraphics.renderItemDecorations(font, item2,40,0)
                        }

                        override fun getProcessor(): IServerDataProcessor = IServerDataProcessor.Companion.FurnaceData

                        override fun onServerDataReceived(buf: RegistryFriendlyByteBuf) {
                            item0 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
                            item1 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
                            item2 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
                        }

                        override fun appendContext(context: ContextHolder, target: FurnaceBlockEntity) {
                            context.append(IContextType.BLOCK_POS, target.blockPos)
                        }
                    }
                    child.add(t as HologramWidgetComponent<T>)
                    val g2 = object : HologramWidgetComponent.Group<T>(child) {

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
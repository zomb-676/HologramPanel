package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.DebugHelper
import com.github.zomb_676.hologrampanel.api.HologramInteractive
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.ItemInteractivePayload
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.render.HologramStyle.Companion.ITEM_STACK_LENGTH
import com.github.zomb_676.hologrampanel.util.InteractiveEntry
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.ScreenPosition
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.util.TooltipType
import com.github.zomb_676.hologrampanel.util.stack
import com.github.zomb_676.hologrampanel.util.stackIf
import com.mojang.blaze3d.platform.Lighting
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.ClientHooks
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
import net.neoforged.neoforge.client.textures.FluidSpriteCache
import net.neoforged.neoforge.fluids.FluidType
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

interface IRenderElement {

    fun measureContentSize(style: HologramStyle): Size
    fun render(style: HologramStyle, partialTicks: Float)

    fun setScale(scale: Double): IRenderElement
    fun getScale(): Double

    fun setPositionOffset(x: Int, y: Int): IRenderElement
    fun getPositionOffset(): ScreenPosition

    fun noCalculateSize(): IRenderElement
    fun hasCalculateSize(): Boolean

    fun additionLayer(): Int
    fun setAdditionLayer(layer: Int): IRenderElement

    var contentSize: Size

    companion object {
        private fun Float.resetNan() = if (this.isNaN()) 0.0f else this

        fun shortDescription(value: Float) = when {
            value < 1e3 -> "%.2f".format(value)
            value < 1e6 -> "%.2fK".format(value / 1e3)
            value < 1e9 -> "%.2fM".format(value / 1e6)
            else -> "%.2fB".format(value / 1e9)
        }
    }

    data object EmptyElement : IRenderElement {
        override fun measureContentSize(style: HologramStyle): Size = Size.ZERO

        override fun render(
            style: HologramStyle, partialTicks: Float
        ) {
        }

        override fun setScale(scale: Double): IRenderElement = this
        override fun getScale(): Double = 1.0

        override fun setPositionOffset(x: Int, y: Int): IRenderElement = this

        override fun getPositionOffset(): ScreenPosition = ScreenPosition.ZERO
        override fun noCalculateSize() = this

        override fun hasCalculateSize(): Boolean = false
        override fun additionLayer(): Int = 0

        override fun setAdditionLayer(layer: Int) = this

        override var contentSize: Size
            get() = Size.ZERO
            set(value) {}
    }

    data class EmptySized(override var contentSize: Size) : IRenderElement {
        override fun measureContentSize(style: HologramStyle): Size = contentSize

        override fun render(style: HologramStyle, partialTicks: Float) {}

        override fun setScale(scale: Double): IRenderElement = this

        override fun getScale(): Double = 1.0

        override fun setPositionOffset(x: Int, y: Int): IRenderElement = this

        override fun getPositionOffset(): ScreenPosition = ScreenPosition.ZERO

        override fun noCalculateSize(): IRenderElement = this

        override fun hasCalculateSize(): Boolean = true

        override fun additionLayer(): Int = 0

        override fun setAdditionLayer(layer: Int): IRenderElement = this
    }

    abstract class RenderElement : IRenderElement {
        final override var contentSize: Size = Size.ZERO

        private var scale: Double = 1.0
            @JvmName("privateScaleSet") set(value) {
                require(value > 0)
                field = value
            }
        private var hasCalculateSize = true

        private var additionLayer = 0

        private var positionOffset: ScreenPosition = ScreenPosition.ZERO

        protected fun Size.scale(): Size {
            if (scale == 1.0) {
                return this
            } else {
                val w = floor(this.width * scale).toInt()
                val h = floor(this.height * scale).toInt()
                return Size.of(w, h)
            }
        }

        final override fun getPositionOffset(): ScreenPosition = this.positionOffset

        final override fun setPositionOffset(x: Int, y: Int): RenderElement {
            this.positionOffset = ScreenPosition.of(x, y)
            return this
        }

        final override fun getScale(): Double = scale

        final override fun setScale(scale: Double): RenderElement {
            this.scale = scale
            return this
        }

        final override fun hasCalculateSize(): Boolean = hasCalculateSize

        final override fun noCalculateSize(): RenderElement {
            this.hasCalculateSize = false
            return this
        }

        final override fun additionLayer(): Int = additionLayer

        final override fun setAdditionLayer(layer: Int): RenderElement {
            this.additionLayer = layer
            return this
        }
    }

    open class EntityRenderElement(val entity: Entity, val entityScale: Double) : RenderElement() {
        companion object {
            val QUATERNION: Quaternionf = Quaternionf().rotateXYZ(Math.toRadians(15.0).toFloat(), 0f, Math.PI.toFloat())
        }

        override fun measureContentSize(style: HologramStyle): Size {
            return Size.of(
                floor(entity.bbWidth * entityScale * 2).toInt(), floor(entity.bbHeight * entityScale).toInt()
            ).expandWidth(2).expandHeight(2).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            val guiGraphics = style.guiGraphics
            style.stack {
                style.translate(contentSize.width.toFloat() / 2, contentSize.height.toFloat(), 50.0f)
                val entityScale = entityScale.toFloat()
                style.scale(entityScale, entityScale, -entityScale)
                style.mulPose(QUATERNION)
                guiGraphics.flush()
                Lighting.setupForEntityInInventory()
                val dispatcher = Minecraft.getInstance().entityRenderDispatcher

                dispatcher.setRenderShadow(false)
                dispatcher.render(
                    entity, 0.0, 0.0, 0.0, 1.0f, guiGraphics.pose(), guiGraphics.bufferSource, LightTexture.FULL_BRIGHT
                )
                guiGraphics.flush()
                dispatcher.setRenderShadow(true)
                Lighting.setupFor3DItems()
            }
        }

        protected fun renderEntityOutline(style: HologramStyle) {
            val colorWhite = -1
            style.guiGraphics
            val centerX = contentSize.width / 2
            val halfWidth = entity.bbWidth * entityScale * getScale()
            val height = entity.bbHeight * entityScale * getScale()
            style.drawHorizontalLine(-1000, 1000, contentSize.height, colorWhite)
            style.drawHorizontalLine(-1000, 1000, (contentSize.height - height).toInt(), colorWhite)
            style.drawVerticalLine(-1000, +1000, (centerX - halfWidth).toInt(), colorWhite)
            style.drawVerticalLine(-1000, +1000, (centerX + halfWidth).toInt(), colorWhite)
        }

        override fun toString(): String {
            return "EntityRenderElement(entity=${entity.javaClass.simpleName}, entityScale=$entityScale)"
        }
    }

    @Deprecated("use the entity variant", replaceWith = ReplaceWith("EntityRenderElement"), DeprecationLevel.HIDDEN)
    open class LivingEntityRenderElement(val entity: LivingEntity, val entityScale: Double) : RenderElement() {
        companion object {
            val QUATERNION: Quaternionf = Quaternionf().rotateZ(Math.PI.toFloat())
            val EMPTY_VECTOR = Vector3f()
        }

        override fun measureContentSize(style: HologramStyle): Size {
            return Size.of(
                floor(entity.bbWidth * entityScale * 2).toInt(), floor(entity.bbHeight * entityScale).toInt()
            ).expandWidth(2).expandHeight(2).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            InventoryScreen.renderEntityInInventory(
                style.guiGraphics,
                contentSize.width.toFloat() / 2,
                contentSize.height.toFloat(),
                entityScale.toFloat(),
                EMPTY_VECTOR,
                QUATERNION,
                null,
                entity
            )
        }

        protected fun renderEntityOutline(style: HologramStyle) {
            val colorWhite = -1
            val graphics = style.guiGraphics
            val centerX = contentSize.width / 2
            val halfWidth = entity.bbWidth * entityScale * getScale()
            val height = entity.bbHeight * entityScale * getScale()
            graphics.hLine(-1000, 1000, contentSize.height, colorWhite)
            graphics.hLine(-1000, 1000, (contentSize.height - height).toInt(), colorWhite)
            graphics.vLine((centerX - halfWidth).toInt(), -1000, +1000, colorWhite)
            graphics.vLine((centerX + halfWidth).toInt(), -1000, +1000, colorWhite)
        }

    }

    open class StringRenderElement(val component: Component) : RenderElement() {
        constructor(string: String) : this(Component.literal(string))

        override fun measureContentSize(style: HologramStyle): Size {
            return style.measureString(component).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            style.drawString(component)
        }

        override fun toString(): String {
            return "String(component=$component)"
        }
    }

    /**
     * similar to [net.minecraft.client.gui.GuiGraphics.renderTooltipInternal]
     */
    open class ScreenTooltipElement(val item: ItemStack, val tooltipType: TooltipType? = null) : RenderElement() {
        var sprite = item.get(DataComponents.TOOLTIP_STYLE)
        var tooltips: List<ClientTooltipComponent> = listOf()
        override fun measureContentSize(style: HologramStyle): Size {
            sprite = item.get(DataComponents.TOOLTIP_STYLE)
            val window = Minecraft.getInstance().window
            tooltips = ClientHooks.gatherTooltipComponents(
                item,
                Screen.getTooltipFromItem(Minecraft.getInstance(), item),
                item.tooltipImage,
                window.guiScaledWidth / 2,
                window.guiScaledWidth,
                window.guiScaledHeight,
                style.font
            )
            var width = 0
            var height = if (tooltips.size == 1) -1 else 0
            tooltips.forEach { tooltip ->
                width = max(width, tooltip.getWidth(style.font))
                height += tooltip.getHeight(style.font)
            }
            return Size.of(width + 6, height + 6).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            val font = style.font
            val texture = ClientHooks.onRenderTooltipTexture(
                item, style.guiGraphics, 0, 0, font, tooltips, sprite
            )
            var height = 0
            style.stack {
                style.guiGraphics.pose().translate(2.0, 2.0, 400.0)
                run {
                    val render = when (tooltipType ?: Config.Style.itemTooltipType.get()) {
                        TooltipType.TEXT, TooltipType.SCREEN_NO_BACKGROUND -> false
                        TooltipType.SCREEN_SMART_BACKGROUND -> texture.texture != null
                        TooltipType.SCREEN_ALWAYS_BACKGROUND -> true
                    }
                    if (render) {
                        TooltipRenderUtil.renderTooltipBackground(
                            style.guiGraphics,
                            0,
                            0,
                            contentSize.width - 4,
                            contentSize.height - 5,
                            0,
                            texture.texture
                        )
                    }
                }

                tooltips.forEachIndexed { index, tooltip ->
                    tooltip.renderText(
                        font, 0, height, style.poseMatrix(), style.guiGraphics.bufferSource
                    )
                    height += tooltip.getHeight(font)
                }
                height = 0
                tooltips.forEachIndexed { index, tooltip ->
                    tooltip.renderImage(
                        font, 0, height, contentSize.width, contentSize.height, style.guiGraphics
                    )
                    height += tooltip.getHeight(font)
                }
            }
        }

        override fun toString(): String {
            return "ScreenTooltip(item=$item)"
        }
    }

    open class ItemStackElement(val renderDecoration: Boolean = true, val itemStack: ItemStack) : RenderElement(), HologramInteractive {

        override fun measureContentSize(style: HologramStyle): Size = style.itemStackSize().scale()

        override fun render(style: HologramStyle, partialTicks: Float) {
            if (itemStack.isEmpty) return
            style.itemFiltered(itemStack)
            if (renderDecoration) {
                style.itemDecoration(itemStack)
            }
        }

        fun smallItem(): ItemStackElement {
            this.setScale(0.5)
            return this
        }

        override fun toString(): String {
            return "ItemStack(renderDecoration=$renderDecoration, itemStack=$itemStack)"
        }

        protected val tooltipElement by lazy {
            ScreenTooltipElement(itemStack, TooltipType.SCREEN_ALWAYS_BACKGROUND)
        }

        override fun renderInteractive(
            style: HologramStyle,
            context: HologramContext,
            widgetSize: Size,
            interactiveSize: Size,
            mouseX: Int,
            mouseY: Int,
            partialTicks: Float,
            renderInteractiveHint: Boolean
        ) {
            if (renderInteractiveHint && !itemStack.isEmpty) {
                style.stack {
                    style.move(widgetSize.width + 10, 0)
                    tooltipElement.contentSize = tooltipElement.measureContentSize(style)
                    tooltipElement.render(style, partialTicks)
                }
            }
        }
    }

    open class InteractiveItemElement(item: ItemStack, val interactiveSlot: Int) : ItemStackElement(true, item),
        HologramInteractive {

        override fun render(style: HologramStyle, partialTicks: Float) {
            if (itemStack.isEmpty) {
                style.outline(style.itemStackSize())
            } else super.render(style, partialTicks)
        }

        override fun onMouseClick(
            player: LocalPlayer,
            data: HologramInteractive.MouseButton,
            context: HologramContext,
            interactiveSize: Size,
            mouseX: Int,
            mouseY: Int
        ): Boolean {
            val isShiftDown = data.modifiers and GLFW.GLFW_MOD_SHIFT != 0
            when (data.button) {
                GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                    val count = if (isShiftDown) itemStack.count else 1
                    if (!this.itemStack.isEmpty) {
                        ItemInteractivePayload.query(itemStack, count, context, interactiveSlot)
                    }
                }

                GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                    if (this.itemStack.isEmpty) {
                        val mainHand = player.mainHandItem
                        if (!mainHand.isEmpty) {
                            val count = if (isShiftDown) mainHand.count else 1
                            ItemInteractivePayload.store(mainHand, count, context, interactiveSlot)
                        }
                    } else {
                        val count = if (isShiftDown) this.itemStack.maxStackSize - this.itemStack.count else 1
                        ItemInteractivePayload.store(this.itemStack, count, context, interactiveSlot)
                    }
                }
            }
            return true
        }
    }

    open class ItemsElement protected constructor(val items: List<ItemStack>, itemEachLine: Int, val addition: Boolean) :
        RenderElement(), HologramInteractive {
        constructor(items: List<ItemStack>, itemEachLine: Int = 7) : this(items, itemEachLine, false)

        companion object {
            const val PADDING = 1
            fun calculateLineCount(count: Int, eachLine: Int) = ceil(count.toDouble() / eachLine).toInt()
        }

        private val count = items.size + if (addition) 1 else 0
        val itemEachLine: Int
        val lineCount: Int

        init {
            val estimateLineCount = calculateLineCount(count, itemEachLine)
            if (!addition) {
                this.itemEachLine = itemEachLine
                this.lineCount = estimateLineCount
            } else {
                if (estimateLineCount * itemEachLine == count) {
                    this.itemEachLine = itemEachLine
                    this.lineCount = estimateLineCount
                } else {
                    val addLineCount = calculateLineCount(count, itemEachLine + 1)
                    val minusLineCount = calculateLineCount(count, itemEachLine - 1)
                    val addDiff = abs(addLineCount * (itemEachLine + 1) - count)
                    val minusDiff = abs(minusLineCount * (itemEachLine - 1) - count)
                    if (addDiff < minusDiff) {
                        this.itemEachLine = itemEachLine + 1
                        this.lineCount = addLineCount
                    } else {
                        this.itemEachLine = itemEachLine - 1
                        this.lineCount = minusLineCount
                    }
                }
            }
        }

        override fun measureContentSize(style: HologramStyle): Size {
            val height = ITEM_STACK_LENGTH * lineCount + PADDING * (lineCount - 1)
            val width = if (lineCount > 1) {
                ITEM_STACK_LENGTH * itemEachLine + PADDING * (itemEachLine - 1)
            } else {
                ITEM_STACK_LENGTH * count + PADDING * (count - 1)
            }
            return Size.of(width, height).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            style.font
            style.stack {
                var i = 0
                items.forEachIndexed { index, item ->
                    if (i == itemEachLine) {
                        i = 0
                        style.pop()
                        style.move(0, ITEM_STACK_LENGTH + PADDING)
                        style.push()
                    }
                    style.itemWithFilteredDecoration(item, 0, 0)
                    style.move(ITEM_STACK_LENGTH + PADDING, 0)
                    i++
                }
            }
        }

        override fun toString(): String {
            return "Items(count=$count, items=${items.joinToString().take(30)})"
        }

        protected fun decodeIndex(mouseX: Int, mouseY: Int): Int {
            val length = ITEM_STACK_LENGTH
            val padding = PADDING
            val index = mouseX / (length + padding) + max(mouseY / (length + padding), 0) * itemEachLine
            return if ((index in 0..<items.size)) {
                index
            } else {
                -1
            }
        }

        private val map: Int2ObjectMap<ScreenTooltipElement> = Int2ObjectOpenHashMap()

        override fun renderInteractive(
            style: HologramStyle,
            context: HologramContext,
            widgetSize: Size,
            interactiveSize: Size,
            mouseX: Int,
            mouseY: Int,
            partialTicks: Float,
            renderInteractiveHint: Boolean
        ) {
            if (renderInteractiveHint) {
                style.stack {
                    style.move(widgetSize.width + 10, 0)
                    val index = decodeIndex(mouseX, mouseY)
                    if (index >= 0) {
                        val tooltip = map.computeIfAbsent(index) {
                            ScreenTooltipElement(items[index], TooltipType.SCREEN_ALWAYS_BACKGROUND)
                        }
                        if (tooltip.item.isEmpty) return@stack
                        tooltip.contentSize = tooltip.measureContentSize(style)
                        tooltip.render(style, partialTicks)
                    }
                }
            }
        }
    }

    open class InteractiveItemsElement(items: List<ItemStack>, itemEachLine: Int = 7, addition: Boolean = true) :
        ItemsElement(items, itemEachLine, addition) {
        override fun onMouseClick(
            player: LocalPlayer,
            data: HologramInteractive.MouseButton,
            context: HologramContext,
            interactiveSize: Size,
            mouseX: Int,
            mouseY: Int
        ): Boolean {
            val index = decodeIndex(mouseX, mouseY)
            val shiftDown = data.modifiers and GLFW.GLFW_MOD_SHIFT != 0
            if (index >= 0) {
                val itemStack = items[index]
                val isShiftDown = data.modifiers and GLFW.GLFW_MOD_SHIFT != 0
                when (data.button) {
                    GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                        if (!itemStack.isEmpty) {
                            val count = if (shiftDown) {
                                val mainHand = player.mainHandItem
                                if (ItemStack.isSameItemSameComponents(mainHand, itemStack)) {
                                    if (mainHand.count == mainHand.maxStackSize) itemStack.maxStackSize
                                    else min(mainHand.maxStackSize - mainHand.count, itemStack.count)
                                } else min(itemStack.maxStackSize, itemStack.count)
                            } else 1
                            ItemInteractivePayload.query(itemStack, count, context, -1)
                        }
                    }

                    GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                        if (itemStack.isEmpty) {
                            val mainHand = player.mainHandItem
                            if (!mainHand.isEmpty) {
                                val count = if (shiftDown) mainHand.count else 1
                                ItemInteractivePayload.store(mainHand, count, context, index)
                            }
                        } else {
                            val count = if (isShiftDown) {
                                val mainHand = player.mainHandItem
                                if (ItemStack.isSameItemSameComponents(mainHand, itemStack)) mainHand.count
                                else itemStack.maxStackSize
                            } else 1
                            ItemInteractivePayload.store(itemStack, count, context, -1)
                        }
                    }
                }
            } else {
                if (addition && data.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    val mainHand = player.mainHandItem
                    if (mainHand.isEmpty) return true
                    val count = if (shiftDown) mainHand.count else 1
                    ItemInteractivePayload.store(mainHand, count, context)
                }
            }

            return true
        }
    }

    class TextureAtlasSpriteRenderElement(val sprite: TextureAtlasSprite) : RenderElement() {
        companion object {
            @Suppress("DEPRECATION")
            val missing: TextureAtlasSprite =
                Minecraft.getInstance().modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(MissingTextureAtlasSprite.getLocation())
        }

        private var width = sprite.contents().width()
        private var height = sprite.contents().height()

        override fun measureContentSize(style: HologramStyle): Size {
            return Size.of(width, height).scale()
        }

        override fun render(
            style: HologramStyle, partialTicks: Float
        ) {
            val size = this.contentSize
            style.guiGraphics.blitSprite(RenderType::guiTextured, sprite, 0, 0, size.width, size.height)
        }

        fun setRenderSize(width: Int, height: Int) {
            this.width = width
            this.height = height
        }

        override fun toString(): String {
            return "Sprite(sprite=$sprite, width=$width, height=$height)"
        }
    }

    abstract class ProgressBarElement(val progress: ProgressData, var barWidth: Float = 98f) : RenderElement() {

        override fun measureContentSize(
            style: HologramStyle
        ): Size {
            return Size.of(floor(barWidth).toInt(), style.font.lineHeight + 2).scale()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            if (this.requireOutlineDecorate()) {
                style.outline(this.contentSize, style.contextColor)
            }

            val percent = progress.percent
            style.stack {
                if (this.requireOutlineDecorate()) {
                    style.move(1, 1)
                }
                val left: Float
                val right: Float
                val height: Float = if (this.requireOutlineDecorate()) {
                    this.contentSize.height - 2
                } else {
                    this.contentSize.height
                }.toFloat()
                if (progress.LTR) {
                    left = 0.0f
                    right = barWidth * percent
                } else {
                    left = (1.0f - percent) * barWidth
                    right = barWidth.toFloat()
                }
                this.fillBar(style, left, right, height, percent)
            }

            val description = getDescription(percent)
            val width = style.measureString(description).width
            style.stack {
                style.translate(0.0, 0.0, 1.0)
                style.drawString(description, (this.contentSize.width - width) / 2, 2)
            }
        }

        abstract fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float)
        open fun getDescription(percent: Float): Component = Component.literal("%.1f%%".format(percent * 100))

        open fun requireOutlineDecorate(): Boolean = false

        override fun toString(): String {
            return "ProgressBar(progress=$progress, barWidth=$barWidth)"
        }
    }

    class EnergyBarElement(progress: ProgressData, barWidth: Float = 60f) : ProgressBarElement(progress, barWidth) {
        override fun requireOutlineDecorate(): Boolean = true

        override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
            style.fill(left, 0f, right, height, 0xffFF6B6B.toInt())
            if (progress.LTR) {
                style.fill(right, 0f, barWidth, height, 0xafDDDDDD.toInt())
            } else {
                style.fill(0f, left, barWidth, height, 0xafDDDDDD.toInt())
            }
        }

        override fun getDescription(percent: Float): Component {
            val current = if (progress.progressCurrent < 1000) {
                "${progress.progressCurrent}"
            } else {
                shortDescription(progress.progressCurrent.toFloat() / 1000)
            }
            val f = if (progress.progressCurrent == progress.progressMax) {
                current
            } else {
                val max = if (progress.progressMax < 1000) {
                    "${progress.progressMax}"
                } else {
                    shortDescription(progress.progressMax.toFloat() / 1000)
                }
                "$current/$max"
            }
            return Component.literal("").append(f).append("FE")
        }

        override fun toString(): String {
            return "EnergyBar,${super.toString()}"
        }
    }

    class FluidBarElement(progress: ProgressData, val fluid: FluidType) : ProgressBarElement(progress) {
        override fun requireOutlineDecorate(): Boolean = true

        override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
            val left = left.toFloat()
            val right = right.toFloat()
            val height = height.toFloat()

            val handle: IClientFluidTypeExtensions = IClientFluidTypeExtensions.of(fluid)
            val tintColor = handle.tintColor
            val sprite: TextureAtlasSprite = FluidSpriteCache.getSprite(handle.stillTexture)

            val matrix = style.poseMatrix()
            val consumer = style.guiGraphics.bufferSource.getBuffer(RenderType.guiTextured(sprite.atlasLocation()))

            val maxU = (((sprite.u1 - sprite.u0) * percent) + sprite.u0).toFloat()
            val maxV = (((sprite.v1 - sprite.v0) * percent) + sprite.v0).toFloat()

            consumer.addVertex(matrix, left, 0f, 0f).setUv(sprite.u0, sprite.v0).setColor(tintColor)
            consumer.addVertex(matrix, left, height, 0f).setUv(sprite.u0, maxV).setColor(tintColor)
            consumer.addVertex(matrix, right, height, 0f).setUv(maxU, maxV).setColor(tintColor)
            consumer.addVertex(matrix, right, 0f, 0f).setUv(maxU, sprite.v0).setColor(tintColor)
        }

        override fun getDescription(percent: Float): Component {
            val current = if (progress.progressCurrent < 1000) {
                "${progress.progressCurrent}mB"
            } else {
                "${shortDescription(progress.progressCurrent.toFloat() / 1000)}B"
            }
            val f = if (progress.progressCurrent == progress.progressMax) {
                current
            } else {
                val max = if (progress.progressMax < 1000) {
                    "${progress.progressMax}mB"
                } else {
                    "${shortDescription(progress.progressMax.toFloat() / 1000)}B"
                }
                "$current/$max"
            }
            val fluidName = fluid.description
            return Component.literal("").append(fluidName).append(" ").append(f)
        }

        override fun toString(): String {
            return "FluidBar(fluid:${fluid.description},${super.toString()})"
        }
    }

    class WorkingArrowProgressBarElement(progress: ProgressData, barWidth: Float = 15f) :
        ProgressBarElement(progress, barWidth) {

        override fun render(style: HologramStyle, partialTicks: Float) {
            val percent = progress.percent.resetNan()
            style.stack {
                style.move(1, 1)
                val height = (this.contentSize.height - 2).toFloat()
                if (progress.LTR) {
                    this.fillBar(style, 0.0f, barWidth * percent, height, percent)
                } else {
                    this.fillBar(style, (1.0f - percent) * barWidth, barWidth, height, percent)
                }
            }
        }

        companion object {
            const val AXIAL_HALF_WIDTH = 1.75f
            const val BASE_COLOR = 0xff5b5b5b.toInt()
            const val FILL_COLOR = -1
        }

        override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
            val left = left.toFloat()
            val right = right.toFloat()
            val height = height.toFloat()
            style.guiGraphics.flush()

            val buffer = style.guiGraphics.bufferSource
            val consumer = buffer.getBuffer(RenderType.gui())
            val width = this.contentSize.width.toFloat()

            val pose = style.poseMatrix()

            val halfHeight = height / 2
            val cuttingPointX = width - (height / 2.0f)
            //draw base
            consumer.addVertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, cuttingPointX, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(BASE_COLOR)

            consumer.addVertex(pose, width, height / 2, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, cuttingPointX, 0.0f, 0.0f).setColor(BASE_COLOR)
            consumer.addVertex(pose, cuttingPointX, height, 0.0f).setColor(BASE_COLOR)
            //another for quad
            consumer.addVertex(pose, cuttingPointX, height, 0.0f).setColor(BASE_COLOR)

            //draw fill
            if (right <= cuttingPointX) {
                consumer.addVertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, right, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, right, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
            } else {
                consumer.addVertex(pose, left, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, left, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, cuttingPointX, halfHeight + AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, cuttingPointX, halfHeight - AXIAL_HALF_WIDTH, 0.0f).setColor(FILL_COLOR)

                consumer.addVertex(pose, cuttingPointX, 0.0f, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, cuttingPointX, height, 0.0f).setColor(FILL_COLOR)
                val remain = width - right
                consumer.addVertex(pose, right, halfHeight + remain, 0.0f).setColor(FILL_COLOR)
                consumer.addVertex(pose, right, halfHeight - remain, 0.0f).setColor(FILL_COLOR)
            }
        }

        override fun toString(): String {
            return "(style=Arrow,${super.toString()})"
        }
    }

    class WorkingCircleProgressElement(val progress: ProgressData, val outRadius: Float, val inRadius: Float) :
        RenderElement() {
        override fun measureContentSize(style: HologramStyle): Size {
            return Size.of(floor(outRadius * 2).toInt())
        }

        companion object {
            const val BASE_COLOR = 0xff88abdc.toInt()
            const val FILL_COLOR = 0xff4786da.toInt()
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            val percent = progress.percent.resetNan()

            style.stack {
                val move = this.contentSize.width / 2f
                style.translate(move, move)
                val end = percent * Math.PI * 2
                if (inRadius > 0.01f) {
                    style.drawTorus(inRadius, outRadius, FILL_COLOR, beginRadian = Math.PI, endRadian = Math.PI - end)
                } else {
                    style.drawCycle(outRadius, BASE_COLOR, beginRadian = Math.PI * 2, endRadian = 0.0)
                    style.drawCycle(outRadius, FILL_COLOR, beginRadian = Math.PI, endRadian = Math.PI - end)
                }
            }
        }

        override fun toString(): String {
            return "(style=Circle, progress:$progress)"
        }
    }

    class VerticalBox(val elements: MutableList<IRenderElement>, val context: HologramContext) : RenderElement() {
        private var baseX = 0
        private val padding = 1
        override fun measureContentSize(style: HologramStyle): Size {
            var width = 0
            var height = 0
            var calculatedSizeElement = 0
            this.elements.forEach {
                it.contentSize = it.measureContentSize(style)
                val offset = it.getPositionOffset()
                if (it.hasCalculateSize()) {
                    calculatedSizeElement++
                    if (offset == ScreenPosition.ZERO) {
                        width = max(it.contentSize.width, width)
                        height += it.contentSize.height
                    } else {
                        height += it.contentSize.height + offset.y
                        if (offset.x < 0) {
                            baseX = max(baseX, -offset.x)
                        }
                        width = max(width, it.contentSize.width + offset.x)
                    }
                }
            }
            height += (calculatedSizeElement - 1) * padding
            return Size.of(width, height)
        }

        override fun render(style: HologramStyle, partialTicks: Float) {
            val inMouse = style.checkMouseInSize(this.contentSize)
            if (inMouse && Config.Client.renderWidgetDebugInfo.get()) {
                style.stack {
                    style.translate(0f, 0f, 100f)
                    style.outline(this.contentSize, 0xff0000ff.toInt())
                }
            }
            if (baseX != 0) {
                style.move(0, baseX)
            }
            this.elements.forEach { element ->
                val offset = element.getPositionOffset()
                val size = element.contentSize
                style.stackIf(offset != ScreenPosition.ZERO, { style.move(offset) }) {
                    style.stackIf(element.getScale() != 1.0, { style.scale(element.getScale()) }) {
                        if (inMouse && style.checkMouseInSize(size)) {
                            DebugHelper.Client.recordHoverElement(element)
                            if (Config.Client.renderWidgetDebugInfo.get()) {
                                style.stack {
                                    style.translate(0f, 0f, 100f)
                                    style.outline(size, 0xff0000ff.toInt())
                                }
                            }
                            if (element is HologramInteractive) {
                                HologramManager.submitInteractive(InteractiveEntry.of(element, context, size, style))
                            }
                        }
                        val addLayer = element.additionLayer()
                        style.stackIf(addLayer != 0, { style.translate(0.0, 0.0, addLayer.toDouble()) }) {
                            element.render(style, partialTicks)
                        }
                    }
                }
                if (element.hasCalculateSize()) {
                    style.move(0, size.height + padding + offset.y)
                }
            }
        }
    }
}
package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.TooltipType
import com.github.zomb_676.hologrampanel.util.stack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.ClientHooks
import kotlin.math.max

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
        return Size.Companion.of(width + 6, height + 6).scale()
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
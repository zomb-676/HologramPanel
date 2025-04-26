package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.TooltipType
import com.github.zomb_676.hologrampanel.util.packed.Size
import com.github.zomb_676.hologrampanel.util.stack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.ForgeHooksClient
import kotlin.math.max

/**
 * similar to [net.minecraft.client.gui.GuiGraphics.renderTooltipInternal]
 */
open class ScreenTooltipElement(val item: ItemStack, val tooltipType: TooltipType? = null) : RenderElement() {
    var tooltips: List<ClientTooltipComponent> = listOf()
    override fun measureContentSize(style: HologramStyle): Size {
        val window = Minecraft.getInstance().window
        val textElements = Screen.getTooltipFromItem(Minecraft.getInstance(), item)
            .filter { Minecraft.getInstance().font.width(it) > 0 }
        tooltips = ForgeHooksClient.gatherTooltipComponents(
            item,
            textElements,
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
            height += tooltip.height
        }
        return Size.Companion.of(width + 6, height + 6).scale()
    }

    override fun render(style: HologramStyle, partialTicks: Float) {
        val font = style.font
        var height = 0
        style.stack {
            style.guiGraphics.pose().translate(2.0, 2.0, 400.0)
            run {
                val render = when (tooltipType ?: Config.Style.itemTooltipType.get()) {
                    TooltipType.TEXT, TooltipType.SCREEN_NO_BACKGROUND -> false
                    TooltipType.SCREEN_BACKGROUND -> true
                }
                if (render) {
                    TooltipRenderUtil.renderTooltipBackground(
                        style.guiGraphics,
                        0,
                        0,
                        contentSize.width - 4,
                        contentSize.height - 5,
                        0,
                    )
                }
            }

            tooltips.forEachIndexed { index, tooltip ->
                tooltip.renderText(
                    font, 0, height, style.poseMatrix(), style.guiGraphics.bufferSource()
                )
                height += tooltip.height
            }
            height = 0
            tooltips.forEachIndexed { index, tooltip ->
                tooltip.renderImage(
                    font, 0, height, style.guiGraphics
                )
                height += tooltip.height
            }
        }
    }

    override fun toString(): String {
        return "ScreenTooltip(item=$item)"
    }
}
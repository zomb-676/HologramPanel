package com.github.zomb_676.hologrampanel.util

enum class TooltipType {
    /**
     * only [net.minecraft.network.chat.Component]
     */
    TEXT,

    /**
     * use [net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent]
     */
    SCREEN_NO_BACKGROUND,

    /**
     * use [net.minecraft.core.component.DataComponents.TOOLTIP_STYLE] if not missing
     */
    SCREEN_SMART_BACKGROUND,

    /**
     * use [net.minecraft.core.component.DataComponents.TOOLTIP_STYLE] always
     */
    SCREEN_ALWAYS_BACKGROUND
}
package com.github.zomb_676.hologrampanel.api

import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.common.ModConfigSpec
import org.jetbrains.annotations.ApiStatus

/**
 * implementation this interface for you class annotated by [HologramPlugin]
 */
interface IHologramPlugin {
    /**
     * @return the identity path for your plugin
     */
    @EfficientConst
    fun location() : ResourceLocation

    /**
     * register and setup settings on both physical sides
     */
    fun registerCommon(register: HologramCommonRegistration) {}

    /**
     * register and setup settings for the physical client only
     */
    fun registerClient(register: HologramClientRegistration) {}

    @ApiStatus.Experimental
    fun registerClientConfig(register : ModConfigSpec.Builder) {}
}
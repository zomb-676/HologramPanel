package com.github.zomb_676.hologrampanel.api

import net.minecraft.resources.ResourceLocation

interface IHologramPlugin {
    @EfficientConst
    fun location() : ResourceLocation

    fun registerCommon(register: HologramCommonRegistration) {}

    fun registerClient(register: HologramClientRegistration) {}
}
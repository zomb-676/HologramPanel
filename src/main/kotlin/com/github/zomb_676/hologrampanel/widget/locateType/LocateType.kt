package com.github.zomb_676.hologrampanel.widget.locateType

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.mojang.serialization.Codec
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3fc

/**
 * describe who the hologram is displayed
 */
sealed interface LocateType {

    fun getScreenSpacePosition(context: HologramContext, partialTick: Float): ScreenPosition

    /**
     * transform [HologramContext.hologramCenterPosition] into minecraft screen space
     */
    fun getSourceScreenSpacePosition(context: HologramContext, partialTick: Float): ScreenPosition =
        MVPMatrixRecorder.transform(getSourceWorldPosition(context, partialTick)).screenPosition

    fun getSourceWorldPosition(context: HologramContext, partialTick: Float): Vector3fc =
        context.hologramCenterPosition(partialTick)

    @ApiStatus.Internal
    fun getLocateEnum(): LocateEnum

    companion object {
        val CODEC: Codec<LocateType> = LocateEnum.ENUM_CODEC.dispatch(LocateType::getLocateEnum, LocateEnum::codec)
    }
}
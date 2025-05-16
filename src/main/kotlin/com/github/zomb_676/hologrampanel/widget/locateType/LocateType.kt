package com.github.zomb_676.hologrampanel.widget.locateType

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.mojang.serialization.Codec
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * describe who the hologram is displayed
 */
sealed interface LocateType {

    /**
     * [HologramContext.hologramCenterPosition]
     */
    @ApiStatus.NonExtendable
    fun getSourceWorldPosition(context: HologramContext, partialTick: Float): Vector3fc =
        context.hologramCenterPosition(partialTick)

    /**
     * consider offset of the `locate` setting
     */
    @ApiStatus.NonExtendable
    fun getLocatedWorldPosition(context: HologramContext, partialTick: Float): Vector3fc =
        applyModifyToWorldPosition(Vector3f(getSourceWorldPosition(context, partialTick)))

    fun applyModifyToWorldPosition(input: Vector3f) : Vector3f = input

    @ApiStatus.Internal
    fun getLocateEnum(): LocateEnum

    companion object {
        val CODEC: Codec<LocateType> = LocateEnum.ENUM_CODEC.dispatch(LocateType::getLocateEnum, LocateEnum::codec)
    }
}
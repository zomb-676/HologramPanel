package com.github.zomb_676.hologrampanel.widget.locateType

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.util.packed.ScreenPosition
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.joml.Vector2f

/**
 * the hologram is rendered totally on screen and is located by screen space location
 */
class LocateOnScreen(val position: Vector2f, var arrange: Boolean = true) : LocateType {
    operator fun component1() = position.x
    operator fun component2() = position.y

    fun setPosition(x: Float, y: Float) {
        position.set(x, y)
    }

    fun getScreenSpacePosition(): ScreenPosition = ScreenPosition.of(position.x, position.y)

    override fun getLocateEnum(): LocateEnum = LocateEnum.SCREEN

    override fun toString(): String {
        return "LocateOnScreen(position=${position.toString(HologramPanel.NUMBER_FORMAT)}, arrange=$arrange)"
    }

    companion object {
        val CODEC: MapCodec<LocateOnScreen> = RecordCodecBuilder.mapCodec { ins ->
            ins.group(
                AllRegisters.Codecs.VEC2F.fieldOf("position").forGetter(LocateOnScreen::position),
                Codec.BOOL.fieldOf("arrange").forGetter(LocateOnScreen::arrange)
            ).apply(ins, ::LocateOnScreen)
        }
    }
}
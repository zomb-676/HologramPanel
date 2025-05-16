package com.github.zomb_676.hologrampanel.widget.locateType

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.HologramPanel
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.joml.Vector3f

/**
 * indicates the hologram always faces the player
 */
class LocateFacingPlayer : LocateInWorld {
    override val offset: Vector3f = Vector3f()

    override fun getLocateEnum(): LocateEnum = LocateEnum.FACING_PLAYER

    override fun toString(): String {
        return "LocateFacingPlayer(offset=${offset.toString(HologramPanel.NUMBER_FORMAT)})"
    }

    companion object {
        val CODEC: MapCodec<LocateFacingPlayer> = RecordCodecBuilder.mapCodec { ins ->
            ins.group(AllRegisters.Codecs.VEC3F.fieldOf("offset").forGetter(LocateFacingPlayer::offset))
                .apply(ins) { offset ->
                    LocateFacingPlayer().also { locate ->
                        locate.offset.set(offset)
                    }
                }
        }
    }
}
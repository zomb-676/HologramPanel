package com.github.zomb_676.hologrampanel.widget.locateType

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.util.StringRepresentable
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class LocateEnum : StringRepresentable {
    FACING_PLAYER {
        override val codec: MapCodec<LocateFacingPlayer> = LocateFacingPlayer.CODEC
    },
    FREELY_IN_WORLD {
        override val codec: MapCodec<LocateFreelyInWorld> = LocateFreelyInWorld.CODEC
    },
    SCREEN {
        override val codec: MapCodec<LocateOnScreen> = LocateOnScreen.CODEC
    };

    abstract val codec: MapCodec<out LocateType>
    override fun getSerializedName(): String = this.name

    companion object {
        val ENUM_CODEC: Codec<LocateEnum> = StringRepresentable.fromEnum(LocateEnum::values)
    }
}
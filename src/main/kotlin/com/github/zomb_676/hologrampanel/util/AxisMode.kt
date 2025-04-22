package com.github.zomb_676.hologrampanel.util;

import com.github.zomb_676.hologrampanel.widget.LocateType
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import org.joml.Vector3f

enum class AxisMode {
    WORLD {
        override fun extractX(player: Player, local: LocateType.World.FacingVector): Vector3f = Vector3f(1f, 0f, 0f)
        override fun extractY(player: Player, local: LocateType.World.FacingVector): Vector3f = Vector3f(1f, 1f, 0f)
        override fun extractZ(player: Player, local: LocateType.World.FacingVector): Vector3f = Vector3f(1f, 0f, 1f)
    },
    LOCAL {
        override fun extractX(player: Player, local: LocateType.World.FacingVector): Vector3f = local.getView().normalize(Vector3f())
        override fun extractY(player: Player, local: LocateType.World.FacingVector): Vector3f = local.getUp().normalize(Vector3f())
        override fun extractZ(player: Player, local: LocateType.World.FacingVector): Vector3f = local.getLeft().normalize(Vector3f())

    },
    PLAYER {
        private inline val camera get() = Minecraft.getInstance().gameRenderer.mainCamera
        override fun extractX(player: Player, local: LocateType.World.FacingVector): Vector3f = camera.lookVector.normalize(Vector3f())
        override fun extractY(player: Player, local: LocateType.World.FacingVector): Vector3f = camera.upVector.normalize(Vector3f())
        override fun extractZ(player: Player, local: LocateType.World.FacingVector): Vector3f = camera.leftVector.normalize(Vector3f())
    };

    /**
     * the returned value can be modified safely
     */
    abstract fun extractX(player: Player, local: LocateType.World.FacingVector): Vector3f

    /**
     * the returned value can be modified safely
     */
    abstract fun extractY(player: Player, local: LocateType.World.FacingVector): Vector3f

    /**
     * the returned value can be modified safely
     */
    abstract fun extractZ(player: Player, local: LocateType.World.FacingVector): Vector3f
}
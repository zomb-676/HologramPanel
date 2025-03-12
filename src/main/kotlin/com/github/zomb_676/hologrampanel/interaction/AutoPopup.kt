package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.PluginManager
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectIterator
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.server.level.BlockDestructionProgress
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import java.util.*
import kotlin.math.sqrt

object AutoPopup {
    const val BLOCK_AUTO_CHECK_DISTANCE = 32.0
    const val ENTITY_AUTO_CHECK_DISTANCE = 32.0
    const val BLOCK_BREAKING_AUTO_CHECK_DISTANCE = 32.0


    fun tick() {
        val pluginManager = PluginManager.getInstance()

        val player = Minecraft.getInstance().player!!
        val level: Level = player.level()
        val centerPosition = player.position()
        level.getEntitiesOfClass(
            Entity::class.java, AABB.ofSize(
                centerPosition, ENTITY_AUTO_CHECK_DISTANCE, ENTITY_AUTO_CHECK_DISTANCE, ENTITY_AUTO_CHECK_DISTANCE
            )
        ).forEach { entity: Entity ->
            if (!HologramManager.checkIdentityExist(entity.uuid)) {
            }
        }

        val iterator: ObjectIterator<Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>>> =
            Minecraft.getInstance().levelRenderer.destructionProgress.long2ObjectEntrySet().iterator()
        val gameMode = Minecraft.getInstance().gameMode
        if (gameMode != null) {
            val localPlayerDestroyingPose = if (gameMode.isDestroying) {
                gameMode.destroyBlockPos
            } else null
            while (iterator.hasNext()) {
                val entry: Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> = iterator.next()
                val pos: BlockPos = BlockPos.of(entry.longKey)
                val distance = sqrt(pos.distToCenterSqr(centerPosition))
                if (distance < BLOCK_BREAKING_AUTO_CHECK_DISTANCE) {
                    val progress = if (pos == localPlayerDestroyingPose) {
                        gameMode.destroyProgress
                    } else {
                        entry.value.last.progress.toFloat() / 10.0f
                    }
                    check(pos, progress)
                }
            }
        }

        BlockPos.betweenClosedStream(
            AABB.ofSize(
                centerPosition, BLOCK_AUTO_CHECK_DISTANCE, BLOCK_AUTO_CHECK_DISTANCE, BLOCK_AUTO_CHECK_DISTANCE
            )
        ).forEach { pos ->

        }
    }

    fun check(pos: BlockPos, progress: Float) {
        //todo
        HologramManager.checkIdentityExist(pos)
    }
}
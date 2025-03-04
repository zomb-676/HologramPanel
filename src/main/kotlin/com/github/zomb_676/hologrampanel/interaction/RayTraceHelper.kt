package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.HologramWidgetAdapter
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.phys.Vec3

object RayTraceHelper {
    private data class InteractionTargetContext(val from: Vec3, val to: Vec3) {
        companion object {
            fun of(player: Player, radius: Int, partialTicks: Float): InteractionTargetContext {
                val eyePosition: Vec3 = player.getEyePosition(partialTicks)
                val viewVector = player.getViewVector(partialTicks)
                val target = eyePosition.add(viewVector.normalize().scale(radius.toDouble()))
                return InteractionTargetContext(eyePosition, target)
            }
        }
    }

    fun findTarget(radius: Int, partialTicks: Float): Pair<BlockPos, Any>? {
        val context = InteractionTargetContext.of(Minecraft.getInstance().player!!, radius, partialTicks)
        val finder = HologramTargetType.Companion.DEFAULTS

        val level = Minecraft.getInstance().level!!

        var finalPos: BlockPos = BlockPos.ZERO
        val target: Any? = BlockGetter.traverseBlocks(context.from, context.to, context, { context, pos ->
            //todo return a list
            finalPos = pos
            finder.firstNotNullOfOrNull { type -> type.extract(pos, level) }
        }, { null })
        return when (target) {
            null -> null
            else -> finalPos to target
        }
    }

    fun <T : Any> createHologramWidget(source: T): HologramWidget {
        val adapters = HologramWidgetAdapter.Companion.defaults
        tailrec fun find(type: Class<*>): HologramWidgetAdapter<*, *> = if (adapters.containsKey(type)) {
            adapters[type]!!
        } else {
            val superClass = type.superclass
            if (superClass == null) {
                HologramWidgetAdapter.Companion.default
            } else {
                find(superClass)
            }
        }

        val adapter = find(source.javaClass).unsafeCast<HologramWidgetAdapter<T, HologramWidget>>()
        return adapter.convert(source)
    }
}
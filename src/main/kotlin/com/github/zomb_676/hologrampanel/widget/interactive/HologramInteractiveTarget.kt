package com.github.zomb_676.hologrampanel.widget.interactive

import com.github.zomb_676.hologrampanel.interaction.HologramState
import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.sync.DataSynchronizer
import com.github.zomb_676.hologrampanel.util.Size
import com.github.zomb_676.hologrampanel.widget.DisplayType
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.FurnaceBlockEntity

abstract class HologramInteractiveTarget(val type: DistType, val synchronizer: DataSynchronizer) {

    /**
     * the generic should be the type this class
     */
    abstract val provider: Provider<*>

    interface Provider<T : HologramInteractiveTarget> {
        fun create(player: Player, type: DistType, synchronizer: DataSynchronizer, buffer: RegistryFriendlyByteBuf): T
    }

    class FurnaceWidget(target: FurnaceTarget) : HologramInteractiveWidget<FurnaceTarget>(target) {
        override val traceSource: Any
            get() = target.furnace.blockPos

        override fun render(state: HologramState, style: HologramStyle, displayType: DisplayType, partialTicks: Float) {
            style.drawString(target.recipeUsedName.toString())
            style.guiGraphics.renderItem(target.item0, 0, 10)
            style.guiGraphics.renderItem(target.item1, 0, 20)
        }

        override fun measure(
            style: HologramStyle, displayType: DisplayType
        ): Size {
            return Size.of(10, 20)
        }
    }

    class FurnaceTarget(type: DistType, synchronizer: DataSynchronizer, pos: BlockPos, player: Player) :
        HologramInteractiveTarget(type, synchronizer) {

        val furnace = player.level().getBlockEntity(pos) as FurnaceBlockEntity

        val recipeUsedName: Long by synchronizer.queryFromServerPerTick(0L, ByteBufCodecs.LONG) {
            System.currentTimeMillis()
        }

        val item0 by synchronizer.queryFromServerPerTick(ItemStack.EMPTY, ItemStack.OPTIONAL_STREAM_CODEC) {
            furnace.getItem(0)
        }
        val item1 by synchronizer.queryFromServerPerTick(ItemStack.EMPTY, ItemStack.OPTIONAL_STREAM_CODEC) {
            furnace.getItem(1)
        }

        override val provider: Provider<*> get() = Furnace
    }

    companion object {
        object Furnace : Provider<FurnaceTarget> {
            override fun create(
                player: Player, type: DistType, synchronizer: DataSynchronizer, buffer: RegistryFriendlyByteBuf
            ): FurnaceTarget {
                val pos = buffer.readBlockPos()
                return FurnaceTarget(type, synchronizer, pos, player)
            }
        }
    }
}
package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.widget.interactive.DistType
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.BlockHitResult
import org.joml.Vector3f
import org.joml.Vector3fc

class BlockHologramContext(
    val pos: BlockPos,
    private val player: Player,
    private val hitResult: BlockHitResult?
) : HologramContext {

    internal val tag = CompoundTag()

    private val centerPosition = Vector3f(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f)

    override fun getLevel(): Level = player.level()

    override fun getPlayer(): Player = player

    override fun getLogicSide(): DistType = DistType.from(getLevel())

    @EfficientConst
    override fun hologramCenterPosition(): Vector3fc = centerPosition

    override fun getIdentityObject(): Any = pos

    override fun getHitContext(): BlockHitResult? = hitResult

    override fun attachedServerData(): CompoundTag = tag

    fun getBlockState(): BlockState = getLevel().getBlockState(pos)

    fun getFluidState(): FluidState = getBlockState().fluidState

    fun getBlockEntity(): BlockEntity? = getLevel().getBlockEntity(pos)

    @JvmName("getBlockEntityWithGenericFilter")
    inline fun <reified T : BlockEntity> getBlockEntity(): T? {
        return getBlockEntity() as T?
    }

    companion object {
        fun of(hit: BlockHitResult, player: Player): BlockHologramContext {
            val pos: BlockPos = hit.blockPos
            return BlockHologramContext(pos, player, hit)
        }
    }
}
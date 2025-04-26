package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import com.github.zomb_676.hologrampanel.util.DistType
import com.github.zomb_676.hologrampanel.widget.dynamic.Remember
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.server.ServerLifecycleHooks
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * context object describing block-based target
 */
class BlockHologramContext(
    val pos: BlockPos,
    private val player: Player,
) : HologramContext {

    private val originalBlock: BlockState = player.level().getBlockState(pos)

    private val centerPosition = Vector3f(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f)
    private val remember = Remember.create(this)

    override fun getLevel(): Level = player.level()

    override fun getPlayer(): Player = player

    override fun getLogicSide(): DistType = DistType.from(getLevel())

    @EfficientConst
    override fun hologramCenterPosition(): Vector3fc = centerPosition

    override fun hologramCenterPosition(partialTick: Float): Vector3fc = hologramCenterPosition()

    /**
     * identity by the [BlockPos]
     */
    override fun getIdentityObject(): Any = pos

    override fun getRememberData(): Remember<BlockHologramContext> = remember

    /**
     * the block state the pos it is at, maybe be different to [createTimeBlockState]
     */
    fun getBlockState(): BlockState = getLevel().getBlockState(pos)

    /**
     * the block state when context created
     */
    @EfficientConst
    fun createTimeBlockState() = originalBlock

    fun getFluidState(): FluidState = getBlockState().fluidState

    fun getBlockEntity(): BlockEntity? = getLevel().getBlockEntity(pos)

    /**
     * type cast type of [getBlockEntity] will return null if not satisfy
     */
    @JvmName("getBlockEntityWithGenericFilter")
    inline fun <reified T : BlockEntity> getBlockEntity(): T? {
        return getBlockEntity() as T?
    }

    /**
     * when the block(not block state) change, we think the widget is invalid
     **/
    override fun stillValid(): Boolean = getBlockState().block == originalBlock.block

    override fun toString(): String {
        return "BlockContext(pos=${pos.toShortString()}, block=${originalBlock.block})"
    }

    companion object {
        fun of(hit: BlockHitResult, player: Player): BlockHologramContext {
            val pos: BlockPos = hit.blockPos
            return BlockHologramContext(pos, player)
        }

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, BlockHologramContext> =
            object : StreamCodec<FriendlyByteBuf, BlockHologramContext> {
                override fun decode(buffer: FriendlyByteBuf): BlockHologramContext {
                    val pos = ByteBufCodecs.BLOCK_POS.decode(buffer)
                    val playerUUID = ByteBufCodecs.UUID.decode(buffer)
                    val player = ServerLifecycleHooks.getCurrentServer()!!.playerList.getPlayer(playerUUID)
                    return BlockHologramContext(pos, player!!)
                }

                override fun encode(
                    buffer: FriendlyByteBuf,
                    value: BlockHologramContext
                ) {
                    ByteBufCodecs.BLOCK_POS.encode(buffer, value.pos)
                    ByteBufCodecs.UUID.encode(buffer, value.player.uuid)
                }
            }
    }
}
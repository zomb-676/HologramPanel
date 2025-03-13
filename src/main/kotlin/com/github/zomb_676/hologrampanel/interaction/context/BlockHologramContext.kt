package com.github.zomb_676.hologrampanel.interaction.context

import com.github.zomb_676.hologrampanel.api.EfficientConst
import com.github.zomb_676.hologrampanel.util.DistType
import com.github.zomb_676.hologrampanel.widget.dynamic.Remember
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.neoforge.server.ServerLifecycleHooks
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3f
import org.joml.Vector3fc
import java.util.*
import kotlin.jvm.optionals.getOrNull

class BlockHologramContext(
    val pos: BlockPos,
    private val player: Player,
    private val hitResult: BlockHitResult?
) : HologramContext {

    private val originalBlock: Block = player.level().getBlockState(pos).block
    private var tag : CompoundTag? = null

    private val centerPosition = Vector3f(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f)
    private val remember = Remember.create(this)

    override fun getLevel(): Level = player.level()

    override fun getPlayer(): Player = player

    override fun getLogicSide(): DistType = DistType.from(getLevel())

    @EfficientConst
    override fun hologramCenterPosition(): Vector3fc = centerPosition

    override fun hologramCenterPosition(partialTick: Float): Vector3fc = hologramCenterPosition()

    override fun getIdentityObject(): Any = pos

    override fun getHitContext(): BlockHitResult? = hitResult

    @ApiStatus.Internal
    override fun attachedServerData(): CompoundTag? = tag

    @ApiStatus.Internal
    override fun setServerUpdateDat(tag: CompoundTag) {
        this.tag = tag
    }

    override fun getRememberData(): Remember<BlockHologramContext> = remember

    fun getBlockState(): BlockState = getLevel().getBlockState(pos)

    fun getFluidState(): FluidState = getBlockState().fluidState

    fun getBlockEntity(): BlockEntity? = getLevel().getBlockEntity(pos)

    @JvmName("getBlockEntityWithGenericFilter")
    inline fun <reified T : BlockEntity> getBlockEntity(): T? {
        return getBlockEntity() as T?
    }

    override fun stillValid(): Boolean = getBlockState().block == originalBlock

    companion object {
        fun of(hit: BlockHitResult, player: Player): BlockHologramContext {
            val pos: BlockPos = hit.blockPos
            return BlockHologramContext(pos, player, hit)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BlockHologramContext> =
            object : StreamCodec<RegistryFriendlyByteBuf, BlockHologramContext> {
                override fun decode(buffer: RegistryFriendlyByteBuf): BlockHologramContext {
                    val pos = BlockPos.STREAM_CODEC.decode(buffer)
                    val playerUUID = UUIDUtil.STREAM_CODEC.decode(buffer)
                    val player = ServerLifecycleHooks.getCurrentServer()!!.playerList.getPlayer(playerUUID)
                    val hit = buffer.readOptional(FriendlyByteBuf::readBlockHitResult)
                    return BlockHologramContext(pos, player!!, hit.getOrNull())
                }

                override fun encode(
                    buffer: RegistryFriendlyByteBuf,
                    value: BlockHologramContext
                ) {
                    BlockPos.STREAM_CODEC.encode(buffer, value.pos)
                    UUIDUtil.STREAM_CODEC.encode(buffer, value.player.uuid)
                    buffer.writeOptional(Optional.ofNullable(value.hitResult), FriendlyByteBuf::writeBlockHitResult)
                }
            }
    }
}
package com.github.zomb_676.hologrampanel.projector

import com.github.zomb_676.hologrampanel.PanelOperatorManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.util.VoxelShapeUtils
import com.github.zomb_676.hologrampanel.util.addClientMessage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class ProjectorBlock(blockProperties: Properties) : Block(blockProperties.noOcclusion().noCollission().strength(0.5f)), EntityBlock {
    companion object {
        val FACING: EnumProperty<Direction> = BlockStateProperties.FACING
        private val AABBs = box(4.0, 0.0, 4.0, 12.0, 3.0, 12.0).let { box ->
            Array(Direction.entries.size) { index ->
                VoxelShapeUtils.rotate(box, Direction.entries[index].opposite)
            }
        }
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        this.defaultBlockState().setValue(FACING, context.clickedFace)

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        AABBs[state.getValue(FACING).ordinal]

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = ProjectorBlockEntity(pos, state)

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        if (!level.isClientSide) return ItemInteractionResult.FAIL
        val storage = level.getCapability(IHologramStorage.CAPABILITY, pos) ?: return ItemInteractionResult.FAIL
        val bind = storage.bindState ?: PanelOperatorManager.selectedTarget
        if (bind == null) {
            addClientMessage("not bind or bound invalid")
            return ItemInteractionResult.FAIL
        } else {
            storage.setAndSyncToServer(bind, pos)
            PanelOperatorManager.selectedTarget = bind
            addClientMessage("success bind data")
            return ItemInteractionResult.SUCCESS
        }
    }
}
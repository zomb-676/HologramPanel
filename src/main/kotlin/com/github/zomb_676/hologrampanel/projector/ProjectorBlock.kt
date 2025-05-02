package com.github.zomb_676.hologrampanel.projector

import com.github.zomb_676.hologrampanel.util.VoxelShapeUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
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
}
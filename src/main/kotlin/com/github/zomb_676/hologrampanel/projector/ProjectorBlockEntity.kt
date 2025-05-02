package com.github.zomb_676.hologrampanel.projector

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContextPrototype
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ProjectorBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(
    AllRegisters.BlockEntities.projectorType.get(), pos, blockState
) {
    val cap = IHologramStorage.DefaultHologramStorage()

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return super.getUpdateTag(registries)
    }

    override fun handleUpdateTag(tag: CompoundTag, lookupProvider: HolderLookup.Provider) {
        super.handleUpdateTag(tag, lookupProvider)
    }

    //change dimension does not call this
    override fun onChunkUnloaded() {
        super.onChunkUnloaded()
        val level = this.level ?: return
        if (level.isClientSide) {
            ProjectorManager.remove(this)
        }
    }

    override fun onLoad() {
        super.onLoad()
        val level = this.level ?: return
        if (level.isClientSide) {
            ProjectorManager.add(this)
        }
    }

    override fun setRemoved() {
        super.setRemoved()
        val level = this.level ?: return
        if (level.isClientSide) {
            ProjectorManager.remove(this)
        }
    }

    fun checkTransform(state: HologramRenderState): Boolean {
        if (state.context !is BlockHologramContext) return false
        val prototype = cap.getStoredPrototype() ?: return false
        if (prototype is HologramContextPrototype.BlockHologramPrototype && prototype.pos == state.context.pos) {

            return true
        }
        return false
    }
}
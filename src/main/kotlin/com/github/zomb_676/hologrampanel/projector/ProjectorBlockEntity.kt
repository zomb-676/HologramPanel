package com.github.zomb_676.hologrampanel.projector

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContextPrototype
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ProjectorBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(
    AllRegisters.BlockEntities.projectorType.get(), pos, blockState
) {
    val cap = IHologramStorage()

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        cap.readFromNbt(tag)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        cap.writeToNBT(tag)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        cap.writeToNBT(tag)
        return tag
    }

    override fun handleUpdateTag(tag: CompoundTag, lookupProvider: HolderLookup.Provider) {
        super.handleUpdateTag(tag, lookupProvider)
        cap.readFromNbt(tag)
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
        val prototype = cap.getStoredPrototype() ?: return false
        val context = state.context
        val res = when (prototype) {
            is HologramContextPrototype.BlockHologramPrototype if context is BlockHologramContext ->
                context.pos == prototype.pos

            is HologramContextPrototype.EntityHologramPrototype if context is EntityHologramContext ->
                context.getEntity().uuid == prototype.entityUUID

            else -> false
        }
        if (res) {
            cap.bindState = state
        }
        return res
    }

    fun setStateLocate(state: HologramRenderState) {
        val old = state.locate
        state.locate = this.cap.getLocateType()
        HologramManager.notifyHologramLocateTypeChange(state, old)
    }
}
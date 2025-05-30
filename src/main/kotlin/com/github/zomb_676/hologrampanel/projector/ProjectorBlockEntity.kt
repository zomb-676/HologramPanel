package com.github.zomb_676.hologrampanel.projector

import com.github.zomb_676.hologrampanel.AllRegisters
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
    /**
     * the [pos] maybe a mutable instance must transform it into an immutable one
     */
    val cap = IHologramStorage(pos.immutable())

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        cap.readFromNbt(tag)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        cap.writeToNBT(tag)
        level?.takeIf { it.isClientSide }?.run {
            cap.onDataSyncedFromServer()
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        cap.writeToNBT(tag)
        return tag
    }

    override fun handleUpdateTag(tag: CompoundTag, lookupProvider: HolderLookup.Provider) {
        super.handleUpdateTag(tag, lookupProvider)
        cap.readFromNbt(tag)
        level?.takeIf { it.isClientSide }?.run {
            cap.onDataSyncedFromServer()
        }
    }

    //change dimension does not call this
    override fun onChunkUnloaded() {
        super.onChunkUnloaded()
        val level = this.level ?: return
        if (level.isClientSide) {
            ProjectorManager.remove(this)
            cap.bindState?.controlled = null
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
            cap.bindState?.controlled = null
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
        cap.setTargetBySelfInfo(state)
    }
}
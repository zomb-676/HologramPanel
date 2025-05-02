package com.github.zomb_676.hologrampanel.projector

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.interaction.context.HologramContextPrototype
import com.github.zomb_676.hologrampanel.widget.LocateType
import net.neoforged.neoforge.capabilities.BlockCapability

interface IHologramStorage {
    companion object {
        val CAPABILITY: BlockCapability<IHologramStorage, Void?> =
            BlockCapability.createVoid(HologramPanel.rl("hologram_store"), IHologramStorage::class.java)
    }

    fun isInControl() : Boolean

    fun getStoredPrototype(): HologramContextPrototype?

    fun storePrototype(context: HologramContextPrototype)

    fun setLocateType(locateType: LocateType)

    fun getLocateType(): LocateType

    class DefaultHologramStorage : IHologramStorage {
        private var prototype: HologramContextPrototype? = null
        private var locateType: LocateType = LocateType.World.FacingPlayer

        override fun isInControl(): Boolean = this.prototype != null

        override fun getStoredPrototype(): HologramContextPrototype? = this.prototype

        override fun storePrototype(prototype: HologramContextPrototype) {
            this.prototype = prototype
        }

        override fun setLocateType(locateType: LocateType) {
            this.locateType = locateType
        }

        override fun getLocateType(): LocateType = this.locateType
    }
}
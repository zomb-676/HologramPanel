package com.github.zomb_676.hologrampanel.projector

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.context.HologramContextPrototype
import com.github.zomb_676.hologrampanel.payload.SetProjectorSettingPayload
import com.github.zomb_676.hologrampanel.widget.locateType.LocateFacingPlayer
import com.github.zomb_676.hologrampanel.widget.locateType.LocateType
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.neoforged.neoforge.capabilities.BlockCapability
import java.util.*

class IHologramStorage(val pos: BlockPos) {
    private var prototype: HologramContextPrototype? = null
    private var locateType: LocateType = LocateFacingPlayer()

    var bindState: HologramRenderState? = null
        get() = if (field.run { this == null || this.removed || !this.sourceGroupVisible() }) {
            field = null
            null
        } else field

    fun getStoredPrototype(): HologramContextPrototype? = this.prototype

    fun storePrototype(prototype: HologramContextPrototype) {
        this.prototype = prototype
    }

    fun setLocateType(locateType: LocateType) {
        this.locateType = locateType
    }

    fun getLocateType(): LocateType = this.locateType

    fun writeToNBT(nbt: CompoundTag) {
        CODEC.encode(this, NbtOps.INSTANCE, CompoundTag()).result().ifPresent {
            nbt.put("hologram_storage", it)
        }
    }

    fun readFromNbt(nbt: CompoundTag) {
        if (nbt.contains("hologram_storage")) {
            val storage = readFromNBT(nbt.getCompound("hologram_storage"))
            this.prototype = storage.prototype
            this.locateType = storage.locateType
        }
    }

    fun setAndSyncToServer(target: HologramRenderState) {
        if (bindState != null && bindState !== target) {
            bindState?.controlled = null
        }
        this.setLocateType(target.locate)
        this.storePrototype(HologramContextPrototype.extract(target.context))
        target.controlled = this
        this.bindState = target
        this.syncToServer()
    }

    fun syncToServer() {
        val tag = CompoundTag()
        this.writeToNBT(tag)
        SetProjectorSettingPayload(tag, pos).sendToServer()
    }

    fun setTargetBySelfInfo(target: HologramRenderState) {
        bindState.takeIf { it === target }?.also { bindState ->
            bindState.controlled = null
        }
        val old = target.locate
        target.locate = this.getLocateType()
        HologramManager.notifyHologramLocateTypeChange(target, old)
        target.controlled = this
    }

    fun onDataSyncedFromServer() {
        val bindState = bindState ?: run {
            val identity = when (val prototype = prototype) {
                is HologramContextPrototype.BlockHologramPrototype -> prototype.pos
                is HologramContextPrototype.EntityHologramPrototype -> prototype.entityUUID
                null -> return
            }
            HologramManager.queryWidgetByIdentity(identity)
        } ?: return
        setTargetBySelfInfo(bindState)
    }

    companion object {
        val CAPABILITY: BlockCapability<IHologramStorage, Void?> =
            BlockCapability.createVoid(HologramPanel.rl("hologram_store"), IHologramStorage::class.java)

        fun readFromNBT(nbt: CompoundTag): IHologramStorage =
            CODEC.decode(NbtOps.INSTANCE, nbt).result().map { it.first }.orElseThrow()

        val CODEC: Codec<IHologramStorage> = RecordCodecBuilder.create { ins ->
            ins.group(
                HologramContextPrototype.CODEC.optionalFieldOf("prototype").forGetter {
                    Optional.ofNullable(it.prototype)
                },
                LocateType.CODEC.fieldOf("locateType").forGetter(IHologramStorage::locateType),
                BlockPos.CODEC.fieldOf("cap_pos").forGetter(IHologramStorage::pos)
            ).apply(ins) { prototype, locateType, pos ->
                IHologramStorage(pos).also { storage ->
                    prototype.ifPresent(storage::storePrototype)
                    storage.locateType = locateType
                }
            }
        }
    }
}
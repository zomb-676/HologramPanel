package com.github.zomb_676.hologrampanel.projector

import com.github.zomb_676.hologrampanel.HologramPanel
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

class IHologramStorage {
    private var prototype: HologramContextPrototype? = null
    private var locateType: LocateType = LocateFacingPlayer()

    var bindState: HologramRenderState? = null
        get() = if (field.run { this == null || this.removed }) {
            field = null
            null
        } else field

    fun isInControl(): Boolean = this.prototype != null

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

    fun setAndSyncToServer(target : HologramRenderState, pos : BlockPos) {
        this.setLocateType(target.locate)
        this.storePrototype(HologramContextPrototype.extract(target.context))
        this.bindState = target
        val tag = CompoundTag()
        this.writeToNBT(tag)
        SetProjectorSettingPayload(tag, pos).sendToServer()
    }

    companion object {
        val CAPABILITY: BlockCapability<IHologramStorage, Void?> =
            BlockCapability.createVoid(HologramPanel.rl("hologram_store"), IHologramStorage::class.java)

        fun readFromNBT(nbt: CompoundTag) =
            CODEC.decode(NbtOps.INSTANCE, nbt).result().map { it.first }.orElse(IHologramStorage())

        val CODEC: Codec<IHologramStorage> = RecordCodecBuilder.create { ins ->
            ins.group(
                HologramContextPrototype.CODEC.optionalFieldOf("prototype").forGetter {
                    Optional.ofNullable(it.prototype)
                },
                LocateType.CODEC.fieldOf("locateType").forGetter(IHologramStorage::locateType),
            ).apply(ins) { prototype, locateType ->
                IHologramStorage().also { storage ->
                    prototype.ifPresent(storage::storePrototype)
                    storage.locateType = locateType
                }
            }
        }
    }
}
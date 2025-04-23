package com.github.zomb_676.hologrampanel.payload

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.api.HologramHolder
import com.github.zomb_676.hologrampanel.interaction.HologramManager
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.RayTraceHelper
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.polyfill.ByteBufCodecs
import com.github.zomb_676.hologrampanel.polyfill.IPayloadContext
import com.github.zomb_676.hologrampanel.polyfill.IPayloadHandler
import com.github.zomb_676.hologrampanel.polyfill.RegistryFriendlyByteBuf
import com.github.zomb_676.hologrampanel.polyfill.StreamCodec
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraftforge.network.NetworkEvent

/**
 * notify the client that a mob is converted into another
 *
 * notice that, due to many reasons, old and new entity can't be guaranteed to be existed at client side
 */
class EntityConversationPayload(val oldEntityID: Int, val newEntityID: Int, val level: ResourceKey<Level>)
    : CustomPacketPayload<EntityConversationPayload> {

    override fun handle(context: NetworkEvent.Context) {
        HANDLE.handle(this, IPayloadContext(context))
    }

    companion object {
        private val paddingEntities: Int2ObjectOpenHashMap<HologramRenderState?> = Int2ObjectOpenHashMap()

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, EntityConversationPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EntityConversationPayload::oldEntityID,
            ByteBufCodecs.VAR_INT, EntityConversationPayload::newEntityID,
            AllRegisters.Codecs.LEVEL_STREAM_CODE, EntityConversationPayload::level,
            ::EntityConversationPayload
        )
        val HANDLE = object : IPayloadHandler<EntityConversationPayload> {
            override fun handle(
                payload: EntityConversationPayload,
                context: IPayloadContext
            ) {
                if (!Config.Client.transformerContextAfterMobConversation.get()) return
                val level = Minecraft.getInstance().level ?: return
                if (level.dimension() != payload.level) return
                //old may have been removed and new may have not been created
                val oldEntity = level.getEntity(payload.oldEntityID)
                val state = HologramManager.queryHologramState((oldEntity as HologramHolder).`hologramPanel$getWidget`())
                paddingEntities.put(payload.newEntityID, state)
            }
        }

        fun onEntityJoin(entity: Entity) {
            require(entity.level().isClientSide)
            if (!paddingEntities.containsKey(entity.id)) return
            val oldState = paddingEntities.remove(entity.id) ?: return
            val newContext = EntityHologramContext(entity, Minecraft.getInstance().player!!, null)
            val newWidget = RayTraceHelper.createHologramWidget(newContext, oldState.displayType) ?: return
            HologramManager.tryAddWidget(newWidget, newContext, oldState.displayType, oldState.hologramTicks)
        }

        fun clear() {
            this.paddingEntities.clear()
        }
    }
}
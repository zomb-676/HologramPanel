package com.github.zomb_676.hologrampanel.addon.universial

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffectUtil
import net.minecraft.world.entity.LivingEntity

data object  LivingEntityPotionProvider : ServerDataProvider<EntityHologramContext, LivingEntity> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: EntityHologramContext
    ): Boolean {
        val entity = context.getEntity<LivingEntity>() ?: return true
        val effects = entity.activeEffects
        val data = context.createRegistryFriendlyByteBuf()
        if (effects.isEmpty()) {
            data.writeVarInt(0)
        } else {
            data.writeVarInt(effects.size)
            effects.forEach { effect ->
                MobEffectInstance.STREAM_CODEC.encode(data, effect)
            }
        }
        targetData.putByteArray("potions", data.asByteBuf().array())
        return true
    }

    /**
     * copied from [net.minecraft.client.gui.screens.inventory.EffectsInInventory.getEffectName]
     */
    private fun getEffectName(effect: MobEffectInstance): Component {
        val component = effect.effect.value().displayName.copy()
        if (effect.amplifier >= 1 && effect.amplifier <= 9) {
            component.append(CommonComponents.SPACE)
                .append(Component.translatable("enchantment.level." + (effect.amplifier + 1)))
        }

        return component
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<EntityHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val entity = context.getEntity<LivingEntity>()
        val remember = context.getRememberData()
        val data by remember.server(0, listOf()) { tag ->
            val data = context.warpRegistryFriendlyByteBuf(tag.getByteArray("potions"))
            val count = data.readVarInt()
            if (count != 0) {
                List(count) {
                    MobEffectInstance.STREAM_CODEC.decode(data)
                }
            } else {
                listOf()
            }
        }
        if (data.isNotEmpty()) {
            builder.group("potions", "potions") {
                val effectTextures = Minecraft.getInstance().mobEffectTextures
                data.forEachIndexed { index, effect ->
                    builder.single("effect_$index") {
                        sprite(effectTextures.get(effect.effect)).setRenderSize(9, 9)
                        component(getEffectName(effect))
                        component(
                            MobEffectUtil.formatDuration(
                                effect,
                                1.0f,
                                Minecraft.getInstance().level!!.tickRateManager().tickrate()
                            )
                        )
                    }
                }
            }
        }
    }

    override fun targetClass(): Class<LivingEntity> = LivingEntity::class.java

    override fun location(): ResourceLocation = HologramPanel.rl("living_entity_potion")

}
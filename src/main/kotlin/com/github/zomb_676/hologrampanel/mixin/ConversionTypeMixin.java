package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.payload.EntityConversationPayload;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"net.minecraft.world.entity.ConversionType$1", "net.minecraft.world.entity.ConversionType$2"})
public class ConversionTypeMixin {
    @SuppressWarnings("resource")
    @Inject(method = "convert", at = @At("HEAD"))
    public void onMobConversation(Mob oldMob, Mob newMob, ConversionParams conversionParams, CallbackInfo ci) {
        if (oldMob == null || newMob == null) return;
        var oldLevel = oldMob.level().dimension();
        if (oldLevel != newMob.level().dimension()) return;
        var payload = new EntityConversationPayload(oldMob.getId(), newMob.getId(), oldLevel);
        PacketDistributor.sendToPlayersTrackingEntity(oldMob, payload);
    }
}

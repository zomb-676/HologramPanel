package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.EventHandler;
import net.minecraftforge.registries.GameData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = GameData.class, remap = false)
public class GameDataMixin {
    @Inject(method = "postRegisterEvents", at = @At("TAIL"))
    private static void onRegisterEnd(CallbackInfo ci) {
        EventHandler.INSTANCE.onRegistryEnd();
    }
}

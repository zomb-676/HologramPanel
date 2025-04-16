package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.render.TransitRenderTargetManager;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "setGuiScale", at = @At("TAIL"))
    private void onSetGuiScale(CallbackInfo ci) {
        TransitRenderTargetManager.INSTANCE.onGuiScaleChange();
    }
}

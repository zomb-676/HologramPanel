package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.util.OpenGLStateManager;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * implementation {@link OpenGLStateManager#preventMainBindWrite}
 */
@Mixin(RenderTarget.class)
public class RenderTargetMixin {
    @Inject(method = "bindWrite", at = @At("HEAD"), cancellable = true)
    public void bindWrite(CallbackInfo ci) {
        RenderTarget target = (RenderTarget) (Object) this;
        if (target instanceof MainTarget && OpenGLStateManager.preventMainBindWrite) {
            ci.cancel();
        }
    }
}

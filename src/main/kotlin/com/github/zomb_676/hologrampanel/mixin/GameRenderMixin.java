package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.EventHandler;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * to be notified when the window resizes, to resize things like frame buffer and attachment stuffs
 */
@Mixin(GameRenderer.class)
public class GameRenderMixin {
    @Inject(method = "resize", at = @At("HEAD"))
    private void onResize(int width, int height, CallbackInfo ci) {
        EventHandler.ClientOnly.INSTANCE.onWindowResize(width, height);
    }
}
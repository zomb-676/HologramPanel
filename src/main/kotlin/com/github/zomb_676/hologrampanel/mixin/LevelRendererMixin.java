package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * record the MVP matrix when projection type is {@link com.mojang.blaze3d.ProjectionType#PERSPECTIVE}
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelTargetBundle;clear()V"))
    public void recordMVPMatrix(CallbackInfo ci) {
        MVPMatrixRecorder.INSTANCE.recordMVPMatrixByCurrentState();
    }
}

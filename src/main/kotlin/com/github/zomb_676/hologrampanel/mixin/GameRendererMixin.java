package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.EventHandler;
import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * to be notified when the window resizes, to resize things like frame buffer and attachment stuffs
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "resize", at = @At("HEAD"))
    private void onResize(int width, int height, CallbackInfo ci) {
        EventHandler.ClientOnly.INSTANCE.onWindowResize(width, height);
    }

    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    public Matrix4f recordProjectionMatrix(Matrix4f projection) {
        MVPMatrixRecorder.INSTANCE.setProjectionMatrix(projection);
        return projection;
    }
}
package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.util.MVPMatrixRecorder;
import com.github.zomb_676.hologrampanel.util.PipRendererRecorder;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

/**
 * to be notified when the window resizes, to resize things like frame buffer and attachment stuffs
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    public Matrix4f recordProjectionMatrix(Matrix4f projection) {
        MVPMatrixRecorder.INSTANCE.setProjectionMatrix(projection);
        return projection;
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>"))
    public List<PictureInPictureRendererRegistration<?>> recordPipRendererFactories(List<PictureInPictureRendererRegistration<?>> pipRendererFactories) {
        PipRendererRecorder.pipRenderers = pipRendererFactories;
        return pipRendererFactories;
    }
}
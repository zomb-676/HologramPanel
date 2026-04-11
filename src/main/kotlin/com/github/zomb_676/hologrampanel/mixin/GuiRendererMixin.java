package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.render.CustomTargetGuiRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements CustomTargetGuiRenderer {
    private RenderTarget customTarget;

    @Override
    public @Nullable RenderTarget getCustomTarget() {
        return customTarget;
    }

    @Override
    public void setCustomTarget(@Nullable RenderTarget renderTarget) {
        this.customTarget = renderTarget;
    }

    @ModifyVariable(method = "draw", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    public RenderTarget replaceTarget(RenderTarget mainTarget) {
        if (customTarget != null) return customTarget;
        return mainTarget;
    }
}
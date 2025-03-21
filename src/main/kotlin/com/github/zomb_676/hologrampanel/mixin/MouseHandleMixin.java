package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.util.selector.CycleSelector;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * prevent player face turn, or stop the effect of mouse move in the world
 */
@Mixin(MouseHandler.class)
public class MouseHandleMixin {
    @WrapWithCondition(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    public boolean wrapTurnPlayer(LocalPlayer instance, double yRot, double xRot) {
        return !CycleSelector.preventPlayerTurn();
    }
}

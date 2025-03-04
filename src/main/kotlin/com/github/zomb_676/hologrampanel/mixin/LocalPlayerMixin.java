package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.interaction.InteractionModeManager;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Shadow
    public ClientInput input;

    @Inject(method = "aiStep", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/player/ClientInput;tick()V"))
    public void onClientInput(CallbackInfo ci) {
        if (InteractionModeManager.INSTANCE.shouldRestPlayerClientInput()) {
            hologramPanel$reset(this.input);
        }
    }

    @Unique
    private static void hologramPanel$reset(ClientInput input) {
        input.keyPresses = Input.EMPTY;
        input.leftImpulse = 0.0f;
        input.forwardImpulse = 0.0f;
    }
}

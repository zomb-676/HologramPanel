package com.github.zomb_676.hologrampanel.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * make opengl debug call back sync, for more convenience debug
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    /**
     * require = 0, not throw when failed
     */
    @Redirect(require = 0, method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V"))
    private void initDebugWithSync(int debugVerbosity, boolean synchronous) {
        if (!FMLLoader.isProduction()) {
            RenderSystem.initRenderer(debugVerbosity, true);
        }
    }
}

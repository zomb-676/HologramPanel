package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.payload.MimicPayload;
import com.github.zomb_676.hologrampanel.util.IgnorePacketException;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * just return {@link MimicPayload} when {@link IgnorePacketException} is thrown
 */
@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public class CustomPacketPayloadMixin {
    @Inject(method = "decode(Lnet/minecraft/network/FriendlyByteBuf;)Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;", cancellable = true,
            at = @At(value = "NEW", target = "(Ljava/lang/String;Ljava/lang/Throwable;)Ljava/lang/RuntimeException;"))
    public <B extends FriendlyByteBuf> void onDecodeFailed(B buffer, CallbackInfoReturnable<CustomPacketPayload> cir, @Local RuntimeException e) {
        if (e instanceof IgnorePacketException) {
            cir.setReturnValue(MimicPayload.INSTANCE);
            buffer.readerIndex(buffer.writerIndex());
            cir.cancel();
        }
    }
}

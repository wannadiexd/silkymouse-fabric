package com.wannadiexd.tapemouse.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    
    @Inject(method = "isWindowFocused", at = @At("HEAD"), cancellable = true)
    private void isWindowFocused(CallbackInfoReturnable<Boolean> cir) {
        // Make the game think the window is always focused for our key simulation
        cir.setReturnValue(true);
    }
}
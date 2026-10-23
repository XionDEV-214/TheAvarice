package com.eisiadev.theavarice.mixin.client;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {

    private static final String THEAVARICE$PREFIX = "enchantment.level.";

    @Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("HEAD"), cancellable = true)
    private void theavarice$arabicLevels(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (key == null || !key.startsWith(THEAVARICE$PREFIX)) return;
        int len = key.length();
        int start = THEAVARICE$PREFIX.length();
        if (len == start || len - start > 10) return;
        for (int i = start; i < len; i++) {
            char ch = key.charAt(i);
            if (ch < '0' || ch > '9') return;
        }
        cir.setReturnValue(key.substring(start));
    }
}

package com.eisiadev.theavarice.mixin;

import com.eisiadev.theavarice.enchant.EnchantNbt;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {

    @Shadow
    @Final
    private Container resultSlots;

    @Inject(method = "createResult", at = @At("RETURN"))
    private void theavarice$sanitizeResult(CallbackInfo ci) {
        EnchantNbt.sanitizePurchased(this.resultSlots.getItem(0));
    }
}

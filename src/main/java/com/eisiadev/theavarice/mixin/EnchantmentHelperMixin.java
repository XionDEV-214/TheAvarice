package com.eisiadev.theavarice.mixin;

import com.eisiadev.theavarice.enchant.CostMath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "getEnchantmentLevel(Lnet/minecraft/nbt/CompoundTag;)I", at = @At("HEAD"), cancellable = true)
    private static void theavarice$uncapLevel(CompoundTag tag, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Mth.clamp(tag.getInt("lvl"), 0, CostMath.ABSOLUTE_MAX_LEVEL));
    }
}

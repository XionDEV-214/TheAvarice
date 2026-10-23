package com.eisiadev.theavarice.compat;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class ApotheosisCompat {

    private static final String HOOKS = "dev.shadowsoffire.apotheosis.ench.asm.EnchHooks";

    private static Boolean loaded;
    private static Method maxLevel;
    private static boolean failed;

    private ApotheosisCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) {
            ModList list = ModList.get();
            if (list == null) return false;
            loaded = list.isLoaded("apotheosis");
        }
        return loaded;
    }

    public static int naturalMaxLevel(Enchantment enchantment) {
        int base = enchantment.getMaxLevel();
        if (failed || !isLoaded()) return base;
        try {
            if (maxLevel == null) maxLevel = Class.forName(HOOKS).getMethod("getMaxLevel", Enchantment.class);
            return Math.max(base, (int) maxLevel.invoke(null, enchantment));
        } catch (Throwable t) {
            failed = true;
            return base;
        }
    }
}

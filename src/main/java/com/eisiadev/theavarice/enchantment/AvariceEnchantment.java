package com.eisiadev.theavarice.enchantment;

import com.eisiadev.theavarice.config.AvariceConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.ForgeRegistries;

public class AvariceEnchantment extends Enchantment {

    public static final int DEFAULT_MAX_LEVEL = 5;

    public static double getMultiplier(int level) {
        return 1.0 + level;
    }

    public static long multipliedCount(int original, double multiplier, double roll) {
        double raw = original * multiplier;
        long count = (long) Math.min(raw, (double) Long.MAX_VALUE / 2);
        if (roll < (raw - Math.floor(raw))) count++;
        return count;
    }

    public static int dropChunk(int maxStackSize) {
        int limit = AvariceConfig.AVARICE_DROP_MERGE_LIMIT.get();
        return limit <= 0 ? Math.max(1, maxStackSize) : limit;
    }

    public static long capDrop(long count, int chunk) {
        return Math.min(count, (long) chunk * AvariceConfig.AVARICE_MAX_DROP_ENTITIES.get());
    }

    public AvariceEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{
                EquipmentSlot.MAINHAND
        });
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        if (super.canEnchant(stack)) return true;
        var rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null && "tacz".equals(rl.getNamespace());
    }

    @Override
    public int getMinLevel() { return 1; }

    @Override
    public int getMaxLevel() { return DEFAULT_MAX_LEVEL; }

    @Override
    public boolean isTreasureOnly() { return true; }

    @Override
    public boolean isTradeable() { return false; }

    @Override
    public boolean isDiscoverable() { return false; }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other);
    }

    @Override
    public int getMinCost(int level) { return 15 + (level - 1) * 9; }

    @Override
    public int getMaxCost(int level) { return getMinCost(level) + 50; }
}

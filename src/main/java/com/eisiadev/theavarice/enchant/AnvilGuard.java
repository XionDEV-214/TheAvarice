package com.eisiadev.theavarice.enchant;

import com.eisiadev.theavarice.config.AvariceConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AnvilGuard {

    private AnvilGuard() {}

    public static boolean process(ItemStack left, ItemStack right, ItemStack out) {
        if (out.isEmpty()) return false;

        Map<ResourceLocation, Integer> l = EnchantNbt.readAll(left);
        Map<ResourceLocation, Integer> r = EnchantNbt.readAll(right);
        if (removesCurse(l, EnchantNbt.readAll(out))) return true;

        Map<ResourceLocation, Integer> o = EnchantNbt.read(out);
        if (o.isEmpty()) {
            EnchantNbt.sanitizePurchased(out);
            return false;
        }

        Map<ResourceLocation, Integer> clamped = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> e : o.entrySet()) {
            int allowed = Math.max(l.getOrDefault(e.getKey(), 0), r.getOrDefault(e.getKey(), 0));
            if (allowed > 0 && e.getValue() > allowed) {
                clamped.put(e.getKey(), allowed);
                e.setValue(allowed);
            }
        }
        if (!clamped.isEmpty()) EnchantNbt.writeLevels(out, clamped);

        Map<ResourceLocation, Integer> pl = EnchantNbt.readPurchased(left);
        Map<ResourceLocation, Integer> pr = EnchantNbt.readPurchased(right);
        Map<ResourceLocation, Integer> po = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> e : o.entrySet()) {
            ResourceLocation id = e.getKey();
            int level = e.getValue();
            int ll = l.getOrDefault(id, 0);
            int rl = r.getOrDefault(id, 0);
            int p;
            if (level == ll && level == rl) p = Math.max(pl.getOrDefault(id, 0), pr.getOrDefault(id, 0));
            else if (level == ll) p = pl.getOrDefault(id, 0);
            else if (level == rl) p = pr.getOrDefault(id, 0);
            else p = 0;
            p = Math.min(p, level);
            if (p > 0) po.put(id, p);
        }
        EnchantNbt.writePurchased(out, po);
        return false;
    }

    private static boolean removesCurse(Map<ResourceLocation, Integer> left, Map<ResourceLocation, Integer> out) {
        boolean enabled;
        try {
            enabled = AvariceConfig.BLOCK_ANVIL_CURSE_REMOVAL.get();
        } catch (IllegalStateException e) {
            return false;
        }
        if (!enabled) return false;
        for (Map.Entry<ResourceLocation, Integer> e : left.entrySet()) {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(e.getKey());
            if (enchantment != null && enchantment.isCurse() && out.getOrDefault(e.getKey(), 0) < e.getValue()) return true;
        }
        return false;
    }

    public static void capCost(DataSlot cost) {
        int cap;
        try {
            cap = AvariceConfig.ANVIL_MAX_COST.get();
        } catch (IllegalStateException e) {
            return;
        }
        if (cap > 0 && cost.get() > cap) cost.set(cap);
    }
}

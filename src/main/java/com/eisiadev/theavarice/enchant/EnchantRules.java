package com.eisiadev.theavarice.enchant;

import com.eisiadev.theavarice.compat.ApotheosisCompat;
import com.eisiadev.theavarice.config.AvariceConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record EnchantRules(CostParams params, Map<ResourceLocation, Integer> caps,
                           Map<ResourceLocation, Integer> naturalMax) {

    public static final Map<String, Integer> DEFAULT_CAPS = Map.ofEntries(
            Map.entry("minecraft:riptide", 7),
            Map.entry("minecraft:quick_charge", 5),
            Map.entry("minecraft:lure", 5),
            Map.entry("minecraft:piercing", 127),
            Map.entry("minecraft:depth_strider", 3),
            Map.entry("minecraft:swift_sneak", 5),
            Map.entry("minecraft:frost_walker", 14),
            Map.entry("minecraft:protection", 20),
            Map.entry("minecraft:fire_protection", 10),
            Map.entry("minecraft:projectile_protection", 10),
            Map.entry("minecraft:feather_falling", 7),
            Map.entry("minecraft:blast_protection", 6),
            Map.entry("minecraft:knockback", 10),
            Map.entry("minecraft:punch", 10),
            Map.entry("minecraft:loyalty", 20),
            Map.entry("minecraft:soul_speed", 10),
            Map.entry("minecraft:mending", 1),
            Map.entry("minecraft:silk_touch", 1),
            Map.entry("minecraft:infinity", 1),
            Map.entry("minecraft:flame", 1),
            Map.entry("minecraft:channeling", 1),
            Map.entry("minecraft:aqua_affinity", 1),
            Map.entry("minecraft:multishot", 1)
    );

    private static EnchantRules cachedServer;
    private static List<Object> cachedSignature;

    public static synchronized EnchantRules server() {
        List<Object> signature = List.of(
                AvariceConfig.UPGRADE_BASE_COST.get(), AvariceConfig.UPGRADE_EXPONENT.get(),
                AvariceConfig.UPGRADE_STEEPNESS.get(), AvariceConfig.REFUND_RATIO.get(),
                AvariceConfig.NATURAL_REFUND_RATIO.get(), AvariceConfig.CURSE_BASE_COST.get(),
                AvariceConfig.WEIGHT_COMMON.get(), AvariceConfig.WEIGHT_UNCOMMON.get(),
                AvariceConfig.WEIGHT_RARE.get(), AvariceConfig.WEIGHT_VERY_RARE.get(),
                AvariceConfig.TREASURE_MULTIPLIER.get(), AvariceConfig.UPGRADE_MAX_LEVEL.get(),
                List.copyOf(AvariceConfig.LEVEL_CAP_OVERRIDES.get()));
        if (cachedServer != null && signature.equals(cachedSignature)) return cachedServer;

        CostParams params = new CostParams(
                AvariceConfig.UPGRADE_BASE_COST.get(), AvariceConfig.UPGRADE_EXPONENT.get(),
                AvariceConfig.UPGRADE_STEEPNESS.get(), AvariceConfig.REFUND_RATIO.get(),
                AvariceConfig.NATURAL_REFUND_RATIO.get(), AvariceConfig.CURSE_BASE_COST.get(),
                new double[]{
                        AvariceConfig.WEIGHT_COMMON.get(), AvariceConfig.WEIGHT_UNCOMMON.get(),
                        AvariceConfig.WEIGHT_RARE.get(), AvariceConfig.WEIGHT_VERY_RARE.get()},
                AvariceConfig.TREASURE_MULTIPLIER.get(), AvariceConfig.UPGRADE_MAX_LEVEL.get());

        Map<ResourceLocation, Integer> caps = new HashMap<>();
        DEFAULT_CAPS.forEach((key, value) -> {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) caps.put(id, value);
        });
        for (String raw : AvariceConfig.LEVEL_CAP_OVERRIDES.get()) {
            String[] parts = raw.split("=");
            if (parts.length != 2) continue;
            ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
            if (id == null) continue;
            try {
                caps.put(id, Math.max(0, Integer.parseInt(parts[1].trim())));
            } catch (NumberFormatException ignored) {
            }
        }

        Map<ResourceLocation, Integer> naturalMax = new HashMap<>();
        if (ApotheosisCompat.isLoaded()) {
            for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS.getValues()) {
                ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                int natural = ApotheosisCompat.naturalMaxLevel(enchantment);
                if (id != null && natural != enchantment.getMaxLevel()) naturalMax.put(id, natural);
            }
        }

        cachedServer = new EnchantRules(params, Map.copyOf(caps), Map.copyOf(naturalMax));
        cachedSignature = signature;
        return cachedServer;
    }

    public int capFor(ResourceLocation id) {
        int global = params.globalMaxLevel();
        Integer cap = caps.get(id);
        return cap == null ? global : Math.min(cap, global);
    }

    public boolean canUpgrade(ResourceLocation id, Enchantment enchantment, int current) {
        return !enchantment.isCurse() && current < capFor(id);
    }

    public int naturalMaxFor(ResourceLocation id, Enchantment enchantment) {
        return Math.max(1, naturalMax.getOrDefault(id, enchantment.getMaxLevel()));
    }

    public CostMath.Curve curve(ResourceLocation id, Enchantment enchantment) {
        double weight = params.weight(enchantment.getRarity().ordinal(), enchantment.isTreasureOnly());
        return new CostMath.Curve(weight, naturalMaxFor(id, enchantment), capFor(id), enchantment.isCurse());
    }
}

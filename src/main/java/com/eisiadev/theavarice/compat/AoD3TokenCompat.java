package com.eisiadev.theavarice.compat;

import com.eisiadev.theavarice.currency.CurrencyGroup;
import com.eisiadev.theavarice.currency.CurrencyTiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Optional Advent of Descension support: each region's tokens become their own wallet chain
 * (base, refined, ancient, empowered, ascendant), each tier worth 100 of the previous one.
 * Regions are discovered from the registry ({@code aod3:<region>_tokens}), so there is no compile-time dependency on aod3.
 */
public final class AoD3TokenCompat {

    public static final String MOD_ID = "aod3";
    public static final String GROUP_PREFIX = "aod3:tokens/";
    public static final long RATE = 100;

    private static final String BASE_SUFFIX = "_tokens";
    /** Lowest to highest value, above the base token. The chain stops at the first tier that isn't registered. */
    private static final String[] TIER_PREFIXES = {"refined", "ancient", "empowered", "ascendant"};

    private AoD3TokenCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static List<CurrencyGroup> build() {
        if (!isLoaded()) return List.of();

        List<ResourceLocation> baseTokens = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            if (id.getNamespace().equals(MOD_ID) && id.getPath().endsWith(BASE_SUFFIX)) baseTokens.add(id);
        }
        baseTokens.sort(Comparator.comparing(ResourceLocation::getPath));

        List<CurrencyGroup> groups = new ArrayList<>();
        for (ResourceLocation baseId : baseTokens) {
            String region = baseId.getPath().substring(0, baseId.getPath().length() - BASE_SUFFIX.length());

            List<CurrencyTiers.Tier> tiers = new ArrayList<>();
            tiers.add(new CurrencyTiers.Tier(baseId, ForgeRegistries.ITEMS.getValue(baseId), 1));

            long value = 1;
            for (String prefix : TIER_PREFIXES) {
                ResourceLocation id = new ResourceLocation(MOD_ID, prefix + "_" + region + "_token");
                if (!ForgeRegistries.ITEMS.containsKey(id)) break;

                value *= RATE;
                tiers.add(new CurrencyTiers.Tier(id, ForgeRegistries.ITEMS.getValue(id), value));
            }

            Collections.reverse(tiers);
            groups.add(new CurrencyGroup(GROUP_PREFIX + region, CurrencyGroup.Category.TOKENS, baseId, List.copyOf(tiers)));
        }
        return List.copyOf(groups);
    }
}

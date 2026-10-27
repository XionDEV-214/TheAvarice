package com.eisiadev.theavarice.compat;

import com.eisiadev.theavarice.currency.CurrencyGroup;
import com.eisiadev.theavarice.currency.CurrencyTiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Optional Advent of Ascension 3 support: its coins become one wallet chain, each coin worth 20 of the previous one.
 * Items are looked up by id, so there is no compile-time dependency on aoa3.
 */
public final class AoA3CoinCompat {

    public static final String MOD_ID = "aoa3";
    public static final String GROUP_KEY = "aoa3:coins";
    public static final long RATE = 20;

    /** Lowest to highest value. The chain stops at the first coin that isn't registered. */
    private static final String[] COINS = {
            "copper_coin",
            "silver_coin",
            "gold_coin",
            "lunaver_coin",
            "ranax_coin",
            "soldena_coin",
            "eclipsia_coin",
            "craeadae_coin"
    };

    private AoA3CoinCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static List<CurrencyGroup> build() {
        if (!isLoaded()) return List.of();

        List<CurrencyTiers.Tier> tiers = new ArrayList<>();
        long value = 1;
        for (String path : COINS) {
            ResourceLocation id = new ResourceLocation(MOD_ID, path);
            if (!ForgeRegistries.ITEMS.containsKey(id)) break;

            tiers.add(new CurrencyTiers.Tier(id, ForgeRegistries.ITEMS.getValue(id), value));
            value *= RATE;
        }
        if (tiers.isEmpty()) return List.of();

        Collections.reverse(tiers);
        return List.of(new CurrencyGroup(GROUP_KEY, CurrencyGroup.Category.COINS, tiers.get(0).id(), List.copyOf(tiers)));
    }
}

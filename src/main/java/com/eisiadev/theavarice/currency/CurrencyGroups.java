package com.eisiadev.theavarice.currency;

import com.eisiadev.theavarice.compat.AoA3CoinCompat;
import com.eisiadev.theavarice.compat.AoD3TokenCompat;
import com.eisiadev.theavarice.config.AvariceConfig;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Every currency chain the wallet handles: the configured Avarice currencies first,
 * then optional compat chains (Advent of Ascension 3 coins, Advent of Descension tokens).
 */
public final class CurrencyGroups {

    private static volatile List<CurrencyGroup> coinCache;
    private static volatile List<CurrencyGroup> tokenCache;

    private CurrencyGroups() {}

    public static List<CurrencyGroup> load() {
        List<CurrencyGroup> groups = new ArrayList<>();

        List<CurrencyTiers.Tier> main = CurrencyTiers.load();
        if (!main.isEmpty()) {
            groups.add(new CurrencyGroup(CurrencyGroup.MAIN_KEY, CurrencyGroup.Category.MAIN, main.get(0).id(), main));
        }

        if (AvariceConfig.COMPAT_AOA3_COINS.get()) groups.addAll(coins());
        if (AvariceConfig.COMPAT_AOD3_TOKENS.get()) groups.addAll(tokens());
        return groups;
    }

    public static CurrencyGroup find(String key) {
        for (CurrencyGroup group : load()) {
            if (group.key().equals(key)) return group;
        }
        return null;
    }

    /** The group that holds {@code item}, or {@code null} if the wallet doesn't handle it. */
    public static CurrencyGroup findByItem(Item item) {
        for (CurrencyGroup group : load()) {
            if (group.find(item) != null) return group;
        }
        return null;
    }

    // Compat chains are built from the item registry, which is frozen once the game is running, so they're cached.
    private static List<CurrencyGroup> coins() {
        List<CurrencyGroup> cached = coinCache;
        if (cached == null) coinCache = cached = AoA3CoinCompat.build();
        return cached;
    }

    private static List<CurrencyGroup> tokens() {
        List<CurrencyGroup> cached = tokenCache;
        if (cached == null) tokenCache = cached = AoD3TokenCompat.build();
        return cached;
    }
}

package com.eisiadev.theavarice.currency;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * A chain of exchangeable currency tiers handled by the wallet as one unit.
 * Tiers are sorted from the most valuable to the least valuable, like {@link CurrencyTiers#load()}.
 */
public record CurrencyGroup(String key, Category category, ResourceLocation icon, List<CurrencyTiers.Tier> tiers) {

    /** Key of the group built from the {@code currencies} config list. */
    public static final String MAIN_KEY = "main";

    /** Which wallet tab a group is shown under. */
    public enum Category {
        MAIN,
        COINS,
        TOKENS
    }

    public CurrencyTiers.Tier find(Item item) {
        for (CurrencyTiers.Tier tier : tiers) {
            if (tier.item() == item) return tier;
        }
        return null;
    }
}

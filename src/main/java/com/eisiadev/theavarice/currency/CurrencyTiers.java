package com.eisiadev.theavarice.currency;

import com.eisiadev.theavarice.config.AvariceConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class CurrencyTiers {

    public record Tier(ResourceLocation id, Item item, long value) {}

    private CurrencyTiers() {}

    public static List<Tier> load() {
        List<Tier> tiers = new ArrayList<>();
        for (String raw : AvariceConfig.CURRENCIES.get()) {
            String[] parts = raw.split(",");
            if (parts.length < 2) return List.of();

            ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
            if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) return List.of();

            long value;
            try {
                value = Long.parseLong(parts[1].trim());
            } catch (NumberFormatException e) {
                return List.of();
            }
            if (value <= 0) return List.of();

            tiers.add(new Tier(id, ForgeRegistries.ITEMS.getValue(id), value));
        }
        tiers.sort((a, b) -> Long.compare(b.value(), a.value()));
        return tiers;
    }

    public static long ratio(long higherValue, long lowerValue) {
        if (lowerValue <= 0 || higherValue % lowerValue != 0) return 0;
        return higherValue / lowerValue;
    }
}

package com.eisiadev.theavarice.client.wallet;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** Coin disc colours inside the tubes, guessed from the item id (gold, silver, ruby...), with a fallback palette. */
public final class TierColors {

    private static final String[] KEYWORDS = {
            "gold", "silver", "copper", "bronze", "brass", "iron", "steel", "platinum", "diamond", "emerald",
            "ruby", "sapphire", "amethyst", "lapis", "quartz", "netherite", "obsidian", "crystal", "lunaver",
            "ranax", "soldena", "eclipsia", "craeadae", "electrum", "nickel", "tin", "lead", "zinc", "titanium"
    };
    private static final int[] COLORS = {
            0xFFF2C230, 0xFFCDD3DB, 0xFFC9753C, 0xFFB5833F, 0xFFD6B04A, 0xFFB9BEC4, 0xFF8E9AA6, 0xFFE3E6EA,
            0xFF5BE3DC, 0xFF35C76A, 0xFFE0335A, 0xFF3A78E0, 0xFFA45BE3, 0xFF2F55B8, 0xFFEDE6DA, 0xFF5A4A4F,
            0xFF3A2B52, 0xFF9DE7F5, 0xFF8AA8FF, 0xFF66D9A0, 0xFFFFD36B, 0xFF9C6BFF, 0xFFFF7AB8, 0xFFE8D27A,
            0xFFB8C9B5, 0xFFC4CCD1, 0xFF7C8796, 0xFFA9B4BE, 0xFFC0C7D0
    };
    /** Fallback by rank (0 = most valuable). */
    private static final int[] BY_RANK = {
            0xFFF2C230, 0xFFCDD3DB, 0xFFC9753C, 0xFF5BE3DC, 0xFFE0335A, 0xFF35C76A, 0xFFA45BE3, 0xFF3A78E0
    };

    private TierColors() {}

    public static int of(ResourceLocation id, int rank) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        for (int i = 0; i < KEYWORDS.length; i++) {
            if (path.contains(KEYWORDS[i])) return COLORS[i];
        }
        return BY_RANK[Math.floorMod(rank, BY_RANK.length)];
    }
}

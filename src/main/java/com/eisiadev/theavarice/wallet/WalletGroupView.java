package com.eisiadev.theavarice.wallet;

import com.eisiadev.theavarice.currency.CurrencyGroup;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Client-side snapshot of one currency group: its tiers (most valuable first) and the player's balance of each. */
public record WalletGroupView(String key, CurrencyGroup.Category category, ResourceLocation icon, List<WalletEntry> entries) {}

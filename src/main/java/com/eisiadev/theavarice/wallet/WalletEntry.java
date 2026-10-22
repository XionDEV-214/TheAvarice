package com.eisiadev.theavarice.wallet;

import net.minecraft.resources.ResourceLocation;

public record WalletEntry(ResourceLocation id, long value, long count) {}

package com.eisiadev.theavarice.wallet;

import com.eisiadev.theavarice.TheAvarice;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class WalletCapability {

    public static final Capability<Wallet> WALLET = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation KEY = new ResourceLocation(TheAvarice.MOD_ID, "wallet");

    private WalletCapability() {}

    public static Wallet get(Player player) {
        return player.getCapability(WALLET).orElseThrow(() -> new IllegalStateException("Wallet capability missing"));
    }
}

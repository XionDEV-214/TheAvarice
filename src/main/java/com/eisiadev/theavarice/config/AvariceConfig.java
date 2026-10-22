package com.eisiadev.theavarice.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class AvariceConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CURRENCIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PICKUP_SOUNDS;
    public static final ForgeConfigSpec.ConfigValue<String> WALLET_TITLE;
    public static final ForgeConfigSpec.ConfigValue<String> WALLET_OPEN_SOUND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CURRENCIES = builder.defineListAllowEmpty("currencies", List.of(
                "gtceu:gtcoin_copper, 1",
                "gtceu:gtcoin_bronze, 100",
                "gtceu:gtcoin_silver, 10000",
                "gtceu:gtcoin_gold, 1000000",
                "gtceu:gtcoin_diamond, 100000000",
                "gtceu:gtcoin_emerald, 10000000000"
        ), o -> o instanceof String);
        PICKUP_SOUNDS = builder.defineListAllowEmpty("pickupSounds", List.of(
                "minecraft:entity.horse.saddle, 1.0, 1.0"
        ), o -> o instanceof String);
        WALLET_TITLE = builder.define("walletTitle", "");
        WALLET_OPEN_SOUND = builder.define("walletOpenSound",
                "minecraft:item.armor.equip_leather, 1.0, 1.0");
        SPEC = builder.build();
    }

    private AvariceConfig() {}
}

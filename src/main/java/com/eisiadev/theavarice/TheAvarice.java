package com.eisiadev.theavarice;

import com.eisiadev.theavarice.config.AvariceConfig;
import com.eisiadev.theavarice.network.WalletNetwork;
import com.eisiadev.theavarice.registry.EnchantmentRegistry;
import com.eisiadev.theavarice.registry.LootModifierRegistry;
import com.eisiadev.theavarice.registry.MenuRegistry;
import com.eisiadev.theavarice.wallet.Wallet;
import com.eisiadev.theavarice.wallet.ui.WalletUIFactory;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod(TheAvarice.MOD_ID)
public class TheAvarice {

    public static final String MOD_ID = "theavarice";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheAvarice() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        EnchantmentRegistry.ENCHANTMENTS.register(bus);
        LootModifierRegistry.GLM.register(bus);
        MenuRegistry.MENUS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AvariceConfig.SPEC);
        bus.addListener(this::commonSetup);
        bus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.register(Wallet.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[TheAvarice] Common setup running.");
        LOGGER.info("[TheAvarice] Avarice enchantment registered: {}",
                ForgeRegistries.ENCHANTMENTS.getKey(EnchantmentRegistry.AVARICE.get()));
        event.enqueueWork(WalletNetwork::register);
        event.enqueueWork(WalletUIFactory::register);
    }
}

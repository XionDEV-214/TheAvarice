package com.eisiadev.theavarice.client;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.network.OpenWalletPacket;
import com.eisiadev.theavarice.network.WalletNetwork;
import com.eisiadev.theavarice.registry.MenuRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public final class WalletKeys {

    public static final KeyMapping OPEN_WALLET = new KeyMapping(
            "key.theavarice.open_wallet",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.categories.theavarice");

    private WalletKeys() {}

    @Mod.EventBusSubscriber(modid = TheAvarice.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {

        private ModEvents() {}

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_WALLET);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(MenuRegistry.WALLET.get(), WalletScreen::new));
        }
    }

    @Mod.EventBusSubscriber(modid = TheAvarice.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeEvents {

        private ForgeEvents() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft minecraft = Minecraft.getInstance();
            while (OPEN_WALLET.consumeClick()) {
                if (minecraft.player != null && minecraft.screen == null) {
                    WalletNetwork.CHANNEL.sendToServer(new OpenWalletPacket());
                }
            }
        }
    }
}

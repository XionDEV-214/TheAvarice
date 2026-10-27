package com.eisiadev.theavarice.network;

import com.eisiadev.theavarice.TheAvarice;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WalletNetwork {

    // 6: enchant page and wallet actions live in the ModularUI wallet screen
    //    (WalletActionPacket, SwitchPagePacket and EnchantApplyPacket removed).
    private static final String VERSION = "6";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TheAvarice.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private WalletNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(OpenWalletPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenWalletPacket::encode)
                .decoder(OpenWalletPacket::decode)
                .consumerMainThread(OpenWalletPacket::handle)
                .add();
        CHANNEL.messageBuilder(WalletSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WalletSyncPacket::encode)
                .decoder(WalletSyncPacket::decode)
                .consumerMainThread(WalletSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(EnchantRulesPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EnchantRulesPacket::encode)
                .decoder(EnchantRulesPacket::decode)
                .consumerMainThread(EnchantRulesPacket::handle)
                .add();
        CHANNEL.messageBuilder(EnchantStatusPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EnchantStatusPacket::encode)
                .decoder(EnchantStatusPacket::decode)
                .consumerMainThread(EnchantStatusPacket::handle)
                .add();
    }
}

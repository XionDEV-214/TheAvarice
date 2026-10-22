package com.eisiadev.theavarice.network;

import com.eisiadev.theavarice.wallet.WalletService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class OpenWalletPacket {

    public static void encode(OpenWalletPacket packet, FriendlyByteBuf buf) {}

    public static OpenWalletPacket decode(FriendlyByteBuf buf) {
        return new OpenWalletPacket();
    }

    public static void handle(OpenWalletPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) return;
        if (player.containerMenu != player.inventoryMenu) return;
        WalletService.open(player);
    }
}

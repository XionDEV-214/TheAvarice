package com.eisiadev.theavarice.network;

import com.eisiadev.theavarice.enchant.EnchantMenu;
import com.eisiadev.theavarice.enchant.EnchantService;
import com.eisiadev.theavarice.wallet.WalletMenu;
import com.eisiadev.theavarice.wallet.WalletService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SwitchPagePacket {

    public enum Page { WALLET, ENCHANT }

    private final Page page;

    public SwitchPagePacket(Page page) {
        this.page = page;
    }

    public static void encode(SwitchPagePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.page);
    }

    public static SwitchPagePacket decode(FriendlyByteBuf buf) {
        return new SwitchPagePacket(buf.readEnum(Page.class));
    }

    public static void handle(SwitchPagePacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) return;
        switch (packet.page) {
            case ENCHANT -> {
                if (player.containerMenu instanceof WalletMenu) EnchantService.open(player);
            }
            case WALLET -> {
                if (player.containerMenu instanceof EnchantMenu) WalletService.open(player);
            }
        }
    }
}

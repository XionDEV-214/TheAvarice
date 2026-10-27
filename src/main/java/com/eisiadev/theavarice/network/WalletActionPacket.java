package com.eisiadev.theavarice.network;

import com.eisiadev.theavarice.wallet.WalletAction;
import com.eisiadev.theavarice.wallet.WalletService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class WalletActionPacket {

    private final String group;
    private final int index;
    private final WalletAction action;

    public WalletActionPacket(String group, int index, WalletAction action) {
        this.group = group;
        this.index = index;
        this.action = action;
    }

    public static void encode(WalletActionPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.group);
        buf.writeVarInt(packet.index);
        buf.writeEnum(packet.action);
    }

    public static WalletActionPacket decode(FriendlyByteBuf buf) {
        return new WalletActionPacket(buf.readUtf(), buf.readVarInt(), buf.readEnum(WalletAction.class));
    }

    public static void handle(WalletActionPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) return;
        WalletService.handle(player, packet.group, packet.index, packet.action);
    }
}

package com.eisiadev.theavarice.network;

import com.eisiadev.theavarice.wallet.ClientWalletState;
import com.eisiadev.theavarice.wallet.WalletEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WalletSyncPacket {

    private final List<WalletEntry> entries;

    public WalletSyncPacket(List<WalletEntry> entries) {
        this.entries = entries;
    }

    public static void encode(WalletSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entries.size());
        for (WalletEntry entry : packet.entries) {
            buf.writeResourceLocation(entry.id());
            buf.writeLong(entry.value());
            buf.writeLong(entry.count());
        }
    }

    public static WalletSyncPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<WalletEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new WalletEntry(buf.readResourceLocation(), buf.readLong(), buf.readLong()));
        }
        return new WalletSyncPacket(entries);
    }

    public static void handle(WalletSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientWalletState.set(packet.entries);
    }
}

package com.eisiadev.theavarice.network;

import com.eisiadev.theavarice.currency.CurrencyGroup;
import com.eisiadev.theavarice.wallet.ClientWalletState;
import com.eisiadev.theavarice.wallet.WalletEntry;
import com.eisiadev.theavarice.wallet.WalletGroupView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WalletSyncPacket {

    private static final int MAX_GROUPS = 512;
    private static final int MAX_TIERS = 64;

    private final List<WalletGroupView> groups;

    public WalletSyncPacket(List<WalletGroupView> groups) {
        this.groups = groups;
    }

    public static void encode(WalletSyncPacket packet, FriendlyByteBuf buf) {
        writeGroups(buf, packet.groups);
    }

    public static WalletSyncPacket decode(FriendlyByteBuf buf) {
        return new WalletSyncPacket(readGroups(buf));
    }

    public static void handle(WalletSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientWalletState.set(packet.groups);
    }

    /** Shared with the wallet UI's open packet, so the screen is built from exactly what the server sees. */
    public static void writeGroups(FriendlyByteBuf buf, List<WalletGroupView> groups) {
        buf.writeVarInt(groups.size());
        for (WalletGroupView group : groups) {
            buf.writeUtf(group.key());
            buf.writeEnum(group.category());
            buf.writeResourceLocation(group.icon());
            buf.writeVarInt(group.entries().size());
            for (WalletEntry entry : group.entries()) {
                buf.writeResourceLocation(entry.id());
                buf.writeLong(entry.value());
                buf.writeLong(entry.count());
            }
        }
    }

    public static List<WalletGroupView> readGroups(FriendlyByteBuf buf) {
        int groupCount = buf.readVarInt();
        if (groupCount < 0 || groupCount > MAX_GROUPS) throw new IllegalArgumentException("Too many wallet groups: " + groupCount);
        List<WalletGroupView> groups = new ArrayList<>(groupCount);
        for (int g = 0; g < groupCount; g++) {
            String key = buf.readUtf();
            CurrencyGroup.Category category = buf.readEnum(CurrencyGroup.Category.class);
            ResourceLocation icon = buf.readResourceLocation();
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_TIERS) throw new IllegalArgumentException("Too many wallet tiers: " + size);
            List<WalletEntry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                entries.add(new WalletEntry(buf.readResourceLocation(), buf.readLong(), buf.readLong()));
            }
            groups.add(new WalletGroupView(key, category, icon, List.copyOf(entries)));
        }
        return List.copyOf(groups);
    }
}

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

    private final List<WalletGroupView> groups;

    public WalletSyncPacket(List<WalletGroupView> groups) {
        this.groups = groups;
    }

    public static void encode(WalletSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.groups.size());
        for (WalletGroupView group : packet.groups) {
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

    public static WalletSyncPacket decode(FriendlyByteBuf buf) {
        int groupCount = buf.readVarInt();
        List<WalletGroupView> groups = new ArrayList<>(groupCount);
        for (int g = 0; g < groupCount; g++) {
            String key = buf.readUtf();
            CurrencyGroup.Category category = buf.readEnum(CurrencyGroup.Category.class);
            ResourceLocation icon = buf.readResourceLocation();
            int size = buf.readVarInt();
            List<WalletEntry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                entries.add(new WalletEntry(buf.readResourceLocation(), buf.readLong(), buf.readLong()));
            }
            groups.add(new WalletGroupView(key, category, icon, List.copyOf(entries)));
        }
        return new WalletSyncPacket(groups);
    }

    public static void handle(WalletSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientWalletState.set(packet.groups);
    }
}

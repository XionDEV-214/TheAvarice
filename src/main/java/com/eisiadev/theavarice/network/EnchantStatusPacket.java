package com.eisiadev.theavarice.network;

import com.eisiadev.theavarice.enchant.ClientEnchantState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class EnchantStatusPacket {

    private final boolean success;
    private final String key;

    public EnchantStatusPacket(boolean success, String key) {
        this.success = success;
        this.key = key;
    }

    public static void encode(EnchantStatusPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.success);
        buf.writeUtf(packet.key, 256);
    }

    public static EnchantStatusPacket decode(FriendlyByteBuf buf) {
        return new EnchantStatusPacket(buf.readBoolean(), buf.readUtf(256));
    }

    public static void handle(EnchantStatusPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientEnchantState.setStatus(packet.success, packet.key);
    }
}

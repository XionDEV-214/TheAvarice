package com.eisiadev.theavarice.network;

import com.eisiadev.theavarice.enchant.EnchantService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class EnchantApplyPacket {

    private static final int MAX_ENTRIES = 256;

    private final List<EnchantService.Request> requests;
    private final BigInteger expectedNet;

    public EnchantApplyPacket(List<EnchantService.Request> requests, BigInteger expectedNet) {
        this.requests = requests;
        this.expectedNet = expectedNet;
    }

    public static void encode(EnchantApplyPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.requests.size());
        for (EnchantService.Request r : packet.requests) {
            buf.writeResourceLocation(r.id());
            buf.writeVarInt(r.target());
        }
        buf.writeByteArray(packet.expectedNet.toByteArray());
    }

    public static EnchantApplyPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) throw new IllegalArgumentException("Too many enchant requests: " + size);
        List<EnchantService.Request> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new EnchantService.Request(buf.readResourceLocation(), buf.readVarInt()));
        }
        BigInteger expected = new BigInteger(buf.readByteArray(64));
        return new EnchantApplyPacket(list, expected);
    }

    public static void handle(EnchantApplyPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) return;
        EnchantService.apply(player, packet.requests, packet.expectedNet);
    }
}

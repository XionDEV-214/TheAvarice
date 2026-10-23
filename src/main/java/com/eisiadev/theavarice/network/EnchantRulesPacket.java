package com.eisiadev.theavarice.network;

import com.eisiadev.theavarice.enchant.ClientEnchantState;
import com.eisiadev.theavarice.enchant.CostParams;
import com.eisiadev.theavarice.enchant.EnchantRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class EnchantRulesPacket {

    private final EnchantRules rules;

    public EnchantRulesPacket(EnchantRules rules) {
        this.rules = rules;
    }

    public static void encode(EnchantRulesPacket packet, FriendlyByteBuf buf) {
        CostParams p = packet.rules.params();
        buf.writeDouble(p.baseCost());
        buf.writeDouble(p.exponent());
        buf.writeDouble(p.steepness());
        buf.writeDouble(p.refundRatio());
        buf.writeDouble(p.naturalRefundRatio());
        buf.writeDouble(p.curseBaseCost());
        buf.writeVarInt(p.rarityWeights().length);
        for (double w : p.rarityWeights()) buf.writeDouble(w);
        buf.writeDouble(p.treasureMultiplier());
        buf.writeVarInt(p.globalMaxLevel());
        writeMap(buf, packet.rules.caps());
        writeMap(buf, packet.rules.naturalMax());
    }

    private static void writeMap(FriendlyByteBuf buf, Map<ResourceLocation, Integer> map) {
        buf.writeVarInt(map.size());
        map.forEach((id, value) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(value);
        });
    }

    private static Map<ResourceLocation, Integer> readMap(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) map.put(buf.readResourceLocation(), buf.readVarInt());
        return Map.copyOf(map);
    }

    public static EnchantRulesPacket decode(FriendlyByteBuf buf) {
        double base = buf.readDouble();
        double exponent = buf.readDouble();
        double steepness = buf.readDouble();
        double refund = buf.readDouble();
        double natural = buf.readDouble();
        double curse = buf.readDouble();
        int n = Math.min(buf.readVarInt(), 16);
        double[] weights = new double[n];
        for (int i = 0; i < n; i++) weights[i] = buf.readDouble();
        double treasure = buf.readDouble();
        int globalMax = buf.readVarInt();
        Map<ResourceLocation, Integer> caps = readMap(buf);
        Map<ResourceLocation, Integer> naturalMax = readMap(buf);
        return new EnchantRulesPacket(new EnchantRules(
                new CostParams(base, exponent, steepness, refund, natural, curse, weights, treasure, globalMax),
                caps, naturalMax));
    }

    public static void handle(EnchantRulesPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientEnchantState.setRules(packet.rules);
    }
}

package com.eisiadev.theavarice.wallet;

import com.eisiadev.theavarice.config.AvariceConfig;
import com.eisiadev.theavarice.currency.CurrencyTiers;
import com.eisiadev.theavarice.network.WalletNetwork;
import com.eisiadev.theavarice.network.WalletSyncPacket;
import com.eisiadev.theavarice.util.SoundHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class WalletService {

    private static final int MAX_WITHDRAW_ITERATIONS = 4096;

    private WalletService() {}

    public static void open(ServerPlayer player) {
        String configured = AvariceConfig.WALLET_TITLE.get();
        Component title = configured.isBlank()
                ? Component.translatable("gui.theavarice.wallet")
                : Component.literal(configured);

        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inventory, p) -> new WalletMenu(id, inventory), title));

        SoundHelper.play(player, AvariceConfig.WALLET_OPEN_SOUND.get());
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        Wallet wallet = WalletCapability.get(player);
        List<WalletEntry> entries = new ArrayList<>();
        for (CurrencyTiers.Tier tier : CurrencyTiers.load()) {
            entries.add(new WalletEntry(tier.id(), tier.value(), wallet.get(tier.id())));
        }
        WalletNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WalletSyncPacket(entries));
    }

    public static long[] values(List<CurrencyTiers.Tier> tiers) {
        long[] out = new long[tiers.size()];
        for (int i = 0; i < out.length; i++) out[i] = tiers.get(i).value();
        return out;
    }

    public static long[] counts(Wallet wallet, List<CurrencyTiers.Tier> tiers) {
        long[] out = new long[tiers.size()];
        for (int i = 0; i < out.length; i++) out[i] = wallet.get(tiers.get(i).id());
        return out;
    }

    public static void setCounts(Wallet wallet, List<CurrencyTiers.Tier> tiers, long[] counts) {
        for (int i = 0; i < counts.length; i++) wallet.set(tiers.get(i).id(), counts[i]);
    }

    public static void handle(ServerPlayer player, int index, WalletAction action) {
        if (!(player.containerMenu instanceof WalletMenu)) return;

        List<CurrencyTiers.Tier> tiers = CurrencyTiers.load();
        if (index < 0 || index >= tiers.size()) return;

        Wallet wallet = WalletCapability.get(player);

        switch (action) {
            case WITHDRAW_ONE -> withdraw(player, wallet, tiers.get(index), 1);
            case WITHDRAW_STACK -> withdraw(player, wallet, tiers.get(index),
                    new ItemStack(tiers.get(index).item()).getMaxStackSize());
            case WITHDRAW_ALL -> withdraw(player, wallet, tiers.get(index), Long.MAX_VALUE);
            case CONVERT_UP -> convertUp(wallet, tiers, index, false);
            case CONVERT_UP_ALL -> convertUp(wallet, tiers, index, true);
            case CONVERT_DOWN -> convertDown(wallet, tiers, index, false);
            case CONVERT_DOWN_ALL -> convertDown(wallet, tiers, index, true);
        }

        sync(player);
    }

    private static void withdraw(ServerPlayer player, Wallet wallet, CurrencyTiers.Tier tier, long requested) {
        long want = Math.min(requested, wallet.get(tier.id()));
        if (want <= 0) return;

        int max = Math.max(1, new ItemStack(tier.item()).getMaxStackSize());
        long moved = 0;
        int iterations = 0;

        while (moved < want && iterations++ < MAX_WITHDRAW_ITERATIONS) {
            int chunk = (int) Math.min(max, want - moved);
            ItemStack stack = new ItemStack(tier.item(), chunk);
            player.getInventory().add(stack);
            moved += chunk - stack.getCount();
            if (!stack.isEmpty()) break;
        }

        wallet.remove(tier.id(), moved);
    }

    private static void convertUp(Wallet wallet, List<CurrencyTiers.Tier> tiers, int index, boolean all) {
        if (index <= 0) return;

        CurrencyTiers.Tier from = tiers.get(index);
        CurrencyTiers.Tier to = tiers.get(index - 1);
        long ratio = CurrencyTiers.ratio(to.value(), from.value());
        if (ratio <= 0) return;

        long possible = wallet.get(from.id()) / ratio;
        long units = all ? possible : Math.min(1, possible);
        if (units <= 0) return;

        wallet.remove(from.id(), units * ratio);
        wallet.add(to.id(), units);
    }

    private static void convertDown(Wallet wallet, List<CurrencyTiers.Tier> tiers, int index, boolean all) {
        if (index + 1 >= tiers.size()) return;

        CurrencyTiers.Tier from = tiers.get(index);
        CurrencyTiers.Tier to = tiers.get(index + 1);
        long ratio = CurrencyTiers.ratio(from.value(), to.value());
        if (ratio <= 0) return;

        long have = wallet.get(from.id());
        long room = (Long.MAX_VALUE - wallet.get(to.id())) / ratio;
        long units = Math.min(all ? have : Math.min(1, have), room);
        if (units <= 0) return;

        wallet.remove(from.id(), units);
        wallet.add(to.id(), units * ratio);
    }
}

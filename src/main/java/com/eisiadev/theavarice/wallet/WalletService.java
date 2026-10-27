package com.eisiadev.theavarice.wallet;

import com.eisiadev.theavarice.config.AvariceConfig;
import com.eisiadev.theavarice.currency.CurrencyGroup;
import com.eisiadev.theavarice.currency.CurrencyGroups;
import com.eisiadev.theavarice.currency.CurrencyTiers;
import com.eisiadev.theavarice.network.WalletNetwork;
import com.eisiadev.theavarice.network.WalletSyncPacket;
import com.eisiadev.theavarice.util.SoundHelper;
import com.eisiadev.theavarice.wallet.ui.WalletContainer;
import com.eisiadev.theavarice.wallet.ui.WalletSession;
import com.eisiadev.theavarice.enchant.EnchantService;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.items.ItemHandlerHelper;
import com.eisiadev.theavarice.wallet.ui.WalletUIFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class WalletService {

    private static final int MAX_WITHDRAW_ITERATIONS = 4096;

    private WalletService() {}

    public static Component title() {
        String configured = AvariceConfig.WALLET_TITLE.get();
        return configured.isBlank()
                ? Component.translatable("gui.theavarice.wallet")
                : Component.literal(configured);
    }

    public static void open(ServerPlayer player) {
        EnchantService.sendRules(player);
        WalletUIFactory.INSTANCE.open(player, snapshot(player), title());
        SoundHelper.play(player, AvariceConfig.WALLET_OPEN_SOUND.get());
    }

    /** Every group the server handles with the player's current balance of each tier, main group first. */
    public static List<WalletGroupView> snapshot(ServerPlayer player) {
        Wallet wallet = WalletCapability.get(player);
        List<WalletGroupView> groups = new ArrayList<>();
        for (CurrencyGroup group : CurrencyGroups.load()) {
            List<WalletEntry> entries = new ArrayList<>();
            for (CurrencyTiers.Tier tier : group.tiers()) {
                entries.add(new WalletEntry(tier.id(), tier.value(), wallet.get(tier.id())));
            }
            groups.add(new WalletGroupView(group.key(), group.category(), group.icon(), List.copyOf(entries)));
        }
        return List.copyOf(groups);
    }

    /** Pushes the snapshot to {@link ClientWalletState} (used by the enchant screen). */
    public static void sync(ServerPlayer player) {
        WalletNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WalletSyncPacket(snapshot(player)));
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

    /**
     * Runs a wallet action requested from the wallet screen. Withdrawn coins go into the screen's coin-out tray;
     * balances reach the client through the screen's sync values, so nothing is sent back from here.
     */
    public static void handle(ServerPlayer player, WalletSession session, String groupKey, int index, WalletAction action) {
        if (!(player.containerMenu instanceof WalletContainer)) return;

        CurrencyGroup group = CurrencyGroups.find(groupKey);
        if (group == null) return;

        List<CurrencyTiers.Tier> tiers = group.tiers();
        if (index < 0 || index >= tiers.size()) return;

        Wallet wallet = WalletCapability.get(player);
        CurrencyTiers.Tier tier = tiers.get(index);

        switch (action) {
            case WITHDRAW_ONE -> withdraw(player, session, wallet, tier, 1);
            case WITHDRAW_STACK -> withdraw(player, session, wallet, tier, new ItemStack(tier.item()).getMaxStackSize());
            case WITHDRAW_ALL -> {
                // "All" means as many as the inventory can take once the tray is collected.
                ItemStack probe = new ItemStack(tier.item());
                long room = inventoryRoom(player, probe) - session.trayCount(probe);
                withdraw(player, session, wallet, tier, Math.max(0, room));
            }
            case CONVERT_UP -> convertUp(wallet, tiers, index, false);
            case CONVERT_UP_ALL -> convertUp(wallet, tiers, index, true);
            case CONVERT_DOWN -> convertDown(wallet, tiers, index, false);
            case CONVERT_DOWN_ALL -> convertDown(wallet, tiers, index, true);
        }
    }

    /** Moves up to {@code requested} coins from the wallet into the tray (as much as the tray holds). */
    private static void withdraw(ServerPlayer player, WalletSession session, Wallet wallet, CurrencyTiers.Tier tier, long requested) {
        long want = Math.min(requested, wallet.get(tier.id()));
        if (want <= 0) return;

        int max = Math.max(1, new ItemStack(tier.item()).getMaxStackSize());
        long moved = 0;
        int iterations = 0;
        while (moved < want && iterations++ < MAX_WITHDRAW_ITERATIONS) {
            int chunk = (int) Math.min(max, want - moved);
            ItemStack rest = ItemHandlerHelper.insertItemStacked(session.tray, new ItemStack(tier.item(), chunk), false);
            moved += chunk - rest.getCount();
            if (!rest.isEmpty()) break;
        }
        wallet.remove(tier.id(), moved);
        if (moved > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.5F, 1.7F);
        }
    }

    /** Free room for {@code stack}'s item in the player's main inventory (36 slots). */
    private static long inventoryRoom(ServerPlayer player, ItemStack stack) {
        long room = 0;
        int max = stack.getMaxStackSize();
        for (ItemStack slot : player.getInventory().items) {
            if (slot.isEmpty()) room += max;
            else if (ItemStack.isSameItemSameTags(slot, stack)) room += Math.max(0, max - slot.getCount());
        }
        return room;
    }

    public static boolean isCurrency(ItemStack stack) {
        return !stack.isEmpty() && !stack.hasTag() && CurrencyGroups.findByItem(stack.getItem()) != null;
    }

    /** Credits currency put into the coin-in slot, like picking it up, then plays the pickup sounds. */
    public static void deposit(ServerPlayer player, ItemStack stack) {
        ItemStack rest = depositSilently(player, stack);
        if (rest.getCount() == stack.getCount()) return;
        CurrencyGroup group = CurrencyGroups.findByItem(stack.getItem());
        if (group != null) WalletCapability.get(player).normalize(group.tiers());
        for (String raw : AvariceConfig.PICKUP_SOUNDS.get()) {
            SoundHelper.play(player, raw);
        }
    }

    /** Adds a currency stack to the wallet without converting. Returns what could not be stored (non-currency). */
    public static ItemStack depositSilently(ServerPlayer player, ItemStack stack) {
        if (!isCurrency(stack)) return stack;
        CurrencyGroup group = CurrencyGroups.findByItem(stack.getItem());
        CurrencyTiers.Tier tier = group.find(stack.getItem());
        WalletCapability.get(player).add(tier.id(), stack.getCount());
        return ItemStack.EMPTY;
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

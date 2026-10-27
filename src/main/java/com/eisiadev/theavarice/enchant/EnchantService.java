package com.eisiadev.theavarice.enchant;

import com.eisiadev.theavarice.currency.CurrencyTiers;
import com.eisiadev.theavarice.currency.WalletMath;
import com.eisiadev.theavarice.network.EnchantRulesPacket;
import com.eisiadev.theavarice.network.EnchantStatusPacket;
import com.eisiadev.theavarice.network.WalletNetwork;
import com.eisiadev.theavarice.wallet.Wallet;
import com.eisiadev.theavarice.wallet.WalletCapability;
import com.eisiadev.theavarice.wallet.WalletService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnchantService {

    private EnchantService() {}

    public record Request(ResourceLocation id, int target) {}

    public static void sendRules(ServerPlayer player) {
        WalletNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnchantRulesPacket(EnchantRules.server()));
    }

    /** Applies the requested level changes to the item in {@code slot} (the wallet screen's enchant slot). */
    public static void apply(ServerPlayer player, IItemHandlerModifiable slot, List<Request> requests, BigInteger expectedNet) {
        ItemStack stack = slot.getStackInSlot(0);
        if (stack.isEmpty()) {
            status(player, false, "gui.theavarice.enchant.error.no_item");
            return;
        }

        EnchantRules rules = EnchantRules.server();
        Map<ResourceLocation, Integer> levels = EnchantNbt.read(stack);
        Map<ResourceLocation, Integer> purchased = EnchantNbt.readPurchased(stack);

        Map<ResourceLocation, Integer> unique = new LinkedHashMap<>();
        for (Request r : requests) unique.put(r.id(), r.target());

        List<ResourceLocation> ids = new ArrayList<>();
        List<CostMath.Change> changes = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> e : unique.entrySet()) {
            ResourceLocation id = e.getKey();
            int target = e.getValue();
            Integer current = levels.get(id);
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
            if (current == null || enchantment == null) {
                status(player, false, "gui.theavarice.enchant.error.not_present");
                return;
            }
            if (target < 0 || target > CostMath.ABSOLUTE_MAX_LEVEL) {
                status(player, false, "gui.theavarice.enchant.error.invalid");
                return;
            }
            if (target > current) {
                if (enchantment.isCurse()) {
                    status(player, false, "gui.theavarice.enchant.error.curse_upgrade");
                    return;
                }
                if (target > rules.capFor(id)) {
                    status(player, false, "gui.theavarice.enchant.error.over_cap");
                    return;
                }
            }
            if (target == current) continue;
            ids.add(id);
            changes.add(new CostMath.Change(rules.curve(id, enchantment), current,
                    purchased.getOrDefault(id, 0), target));
        }
        if (changes.isEmpty()) {
            status(player, false, "gui.theavarice.enchant.error.nothing");
            return;
        }

        boolean creative = player.getAbilities().instabuild;
        CostMath.Quote quote = CostMath.quote(rules.params(), changes, creative);
        BigInteger net = quote.net();

        if (!creative && expectedNet != null && net.compareTo(expectedNet) > 0) {
            status(player, false, "gui.theavarice.enchant.error.quote_changed");
            WalletNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnchantRulesPacket(rules));
            return;
        }

        if (!creative && net.signum() != 0) {
            List<CurrencyTiers.Tier> tiers = CurrencyTiers.load();
            if (tiers.isEmpty()) {
                status(player, false, "gui.theavarice.enchant.error.no_currency");
                return;
            }
            Wallet wallet = WalletCapability.get(player);
            long[] values = WalletService.values(tiers);
            long[] counts = WalletService.counts(wallet, tiers);
            long[] after = net.signum() > 0
                    ? WalletMath.pay(counts, values, net)
                    : WalletMath.deposit(counts, values, net.negate());
            if (after == null) {
                status(player, false, "gui.theavarice.enchant.error.insufficient");
                return;
            }
            WalletService.setCounts(wallet, tiers, after);
        }

        Map<ResourceLocation, Integer> newLevels = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            CostMath.Result result = quote.results().get(i);
            newLevels.put(ids.get(i), result.newLevel());
            if (result.newPurchased() > 0) purchased.put(ids.get(i), result.newPurchased());
            else purchased.remove(ids.get(i));
        }

        ItemStack edited = stack.copy();
        EnchantNbt.writeLevels(edited, newLevels);
        EnchantNbt.writePurchased(edited, purchased);
        slot.setStackInSlot(0, EnchantNbt.finish(edited));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
        status(player, true, net.signum() > 0 && !creative ? "gui.theavarice.enchant.done.paid"
                : net.signum() < 0 && !creative ? "gui.theavarice.enchant.done.refunded"
                : "gui.theavarice.enchant.done");
    }

    private static void status(ServerPlayer player, boolean success, String key) {
        WalletNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnchantStatusPacket(success, key));
    }
}

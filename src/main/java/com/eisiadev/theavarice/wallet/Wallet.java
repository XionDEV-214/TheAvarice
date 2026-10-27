package com.eisiadev.theavarice.wallet;

import com.eisiadev.theavarice.currency.CurrencyTiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Wallet {

    private final Map<ResourceLocation, Long> balances = new LinkedHashMap<>();
    /** Whether currency picked up from the ground goes straight into the wallet. */
    private boolean autoDeposit = true;
    /**
     * Coins withdrawn into the wallet screen's coin-out chute and not taken yet. Kept with the player, so they stay
     * in the chute when the screen is closed and reopened.
     */
    private final ItemStackHandler tray = new ItemStackHandler(TRAY_SLOTS);

    public static final int TRAY_SLOTS = 30;

    public ItemStackHandler tray() {
        return tray;
    }

    public boolean autoDeposit() {
        return autoDeposit;
    }

    public void setAutoDeposit(boolean value) {
        autoDeposit = value;
    }

    public long get(ResourceLocation id) {
        return balances.getOrDefault(id, 0L);
    }

    public void set(ResourceLocation id, long value) {
        if (value <= 0) {
            balances.remove(id);
        } else {
            balances.put(id, value);
        }
    }

    public void add(ResourceLocation id, long amount) {
        if (amount <= 0) return;
        long sum = get(id) + amount;
        set(id, sum < 0 ? Long.MAX_VALUE : sum);
    }

    public long remove(ResourceLocation id, long amount) {
        if (amount <= 0) return 0;
        long current = get(id);
        long removed = Math.min(current, amount);
        set(id, current - removed);
        return removed;
    }

    public void normalize(List<CurrencyTiers.Tier> tiers) {
        for (int i = tiers.size() - 1; i >= 1; i--) {
            CurrencyTiers.Tier lower = tiers.get(i);
            CurrencyTiers.Tier higher = tiers.get(i - 1);
            long ratio = CurrencyTiers.ratio(higher.value(), lower.value());
            if (ratio <= 0) continue;

            long balance = get(lower.id());
            long carry = balance / ratio;
            if (carry <= 0) continue;

            set(lower.id(), balance % ratio);
            add(higher.id(), carry);
        }
    }

    public void copyFrom(Wallet other) {
        balances.clear();
        balances.putAll(other.balances);
        autoDeposit = other.autoDeposit;
        for (int i = 0; i < TRAY_SLOTS; i++) tray.setStackInSlot(i, other.tray.getStackInSlot(i).copy());
    }

    public CompoundTag save() {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, Long> entry : balances.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", entry.getKey().toString());
            tag.putLong("count", entry.getValue());
            list.add(tag);
        }
        CompoundTag root = new CompoundTag();
        root.put("balances", list);
        root.putBoolean("autoDeposit", autoDeposit);
        root.put("tray", tray.serializeNBT());
        return root;
    }

    public void load(CompoundTag root) {
        balances.clear();
        autoDeposit = !root.contains("autoDeposit") || root.getBoolean("autoDeposit");
        for (int i = 0; i < TRAY_SLOTS; i++) tray.setStackInSlot(i, ItemStack.EMPTY);
        if (root.contains("tray", Tag.TAG_COMPOUND)) {
            CompoundTag saved = root.getCompound("tray");
            saved.putInt("Size", TRAY_SLOTS);
            tray.deserializeNBT(saved);
        }
        ListTag list = root.getList("balances", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id == null) continue;
            set(id, tag.getLong("count"));
        }
    }
}

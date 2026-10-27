package com.eisiadev.theavarice.wallet.ui;

import com.eisiadev.theavarice.enchant.EnchantNbt;
import com.eisiadev.theavarice.wallet.WalletService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Item storage that lives while one wallet screen is open, on both sides:
 * <ul>
 *     <li>{@link #tray}: coin-out tray. Withdrawn coins wait here until the player takes them (click / shift-click /
 *     click the chute) or they are collected automatically after a few seconds.</li>
 *     <li>{@link #deposit}: coin-in slot. Anything put in that is currency goes straight into the wallet.</li>
 *     <li>{@link #target}: the enchant page's item slot.</li>
 * </ul>
 * When the screen closes, coins still in the tray go to the inventory (or back into the wallet if it is full) and
 * the enchant item is returned.
 */
public final class WalletSession {

    public static final int TRAY_SLOTS = 6;
    /** Ticks without tray activity before coins in the tray are moved to the inventory. */
    public static final int AUTO_COLLECT_TICKS = 100;

    private final Player player;
    private final boolean client;
    private int idleTicks;
    private boolean closed;

    public final ItemStackHandler tray = new ItemStackHandler(TRAY_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            idleTicks = 0;
        }
    };

    public final ItemStackHandler target = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isEnchanted(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public final DepositHandler deposit;

    public WalletSession(Player player, Predicate<ItemStack> isCurrency) {
        this.player = player;
        this.client = player.level().isClientSide;
        this.deposit = new DepositHandler(isCurrency);
    }

    public static boolean isEnchanted(ItemStack stack) {
        return !stack.isEmpty() && !EnchantNbt.read(stack).isEmpty();
    }

    public boolean trayEmpty() {
        for (int i = 0; i < TRAY_SLOTS; i++) if (!tray.getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    /** How many of {@code stack}'s item are already waiting in the tray. */
    public long trayCount(ItemStack stack) {
        long total = 0;
        for (int i = 0; i < TRAY_SLOTS; i++) {
            ItemStack s = tray.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(s, stack)) total += s.getCount();
        }
        return total;
    }

    /** Server tick: collect the tray once it has been left alone for a while. */
    public void serverTick() {
        if (client || closed || trayEmpty()) {
            idleTicks = 0;
            return;
        }
        if (++idleTicks >= AUTO_COLLECT_TICKS) {
            collect();
            idleTicks = 0;
        }
    }

    /** Moves everything in the tray that fits into the player's inventory. */
    public void collect() {
        if (client) return;
        boolean moved = false;
        for (int i = 0; i < TRAY_SLOTS; i++) {
            ItemStack stack = tray.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ItemStack rest = stack.copy();
            player.getInventory().add(rest);
            if (rest.getCount() != stack.getCount()) {
                moved = true;
                tray.setStackInSlot(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
            }
        }
        if (moved) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
        }
    }

    /** Screen closed (also on logout / death): nothing may be lost. Safe to call more than once. */
    public void close() {
        if (client || closed) return;
        closed = true;
        collect();
        if (player instanceof ServerPlayer serverPlayer) {
            for (int i = 0; i < TRAY_SLOTS; i++) {
                ItemStack stack = tray.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                ItemStack rest = WalletService.depositSilently(serverPlayer, stack);
                if (!rest.isEmpty()) player.getInventory().placeItemBackInInventory(rest);
                tray.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        ItemStack item = target.getStackInSlot(0);
        if (!item.isEmpty()) {
            target.setStackInSlot(0, ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(item);
        }
    }

    /**
     * A one-slot "coin slot" that is always empty: currency put into it is credited to the wallet on the server.
     * On the client it only accepts the same items, so the prediction matches.
     */
    public final class DepositHandler implements IItemHandlerModifiable {

        private final Predicate<ItemStack> isCurrency;

        private DepositHandler(Predicate<ItemStack> isCurrency) {
            this.isCurrency = isCurrency;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (stack.isEmpty() || !isItemValid(slot, stack)) return;
            accept(stack);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
            if (!simulate) accept(stack);
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return !stack.isEmpty() && !stack.hasTag() && isCurrency.test(stack);
        }

        private void accept(ItemStack stack) {
            if (!client && !closed && player instanceof ServerPlayer serverPlayer) {
                WalletService.deposit(serverPlayer, stack.copy());
            }
        }
    }
}

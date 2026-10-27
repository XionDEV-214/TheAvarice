package com.eisiadev.theavarice.wallet.ui;

import com.eisiadev.theavarice.enchant.EnchantNbt;
import com.eisiadev.theavarice.wallet.Wallet;
import com.eisiadev.theavarice.wallet.WalletCapability;
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
 *     click the chute). On the server this is the player's own {@link Wallet#tray()}, so coins left in the chute
 *     stay there when the screen is closed and are back in the same place when it is reopened.</li>
 *     <li>{@link #deposit}: coin-in slot. Anything put in that is currency goes straight into the wallet.</li>
 *     <li>{@link #target}: the enchant page's item slot.</li>
 * </ul>
 * When the screen closes, the enchant item is returned; the tray is left as it is.
 */
public final class WalletSession {

    public static final int TRAY_SLOTS = Wallet.TRAY_SLOTS;

    private final Player player;
    private final boolean client;
    private boolean closed;

    public final ItemStackHandler tray;
    /** Client: tray slots that were filled by the first sync after opening (no falling animation for those). */
    private final boolean[] initial = new boolean[TRAY_SLOTS];

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
        this.tray = client ? new ItemStackHandler(TRAY_SLOTS)
                : player.getCapability(WalletCapability.WALLET).map(Wallet::tray).orElseGet(() -> new ItemStackHandler(TRAY_SLOTS));
        this.deposit = new DepositHandler(isCurrency);
    }

    public static boolean isEnchanted(ItemStack stack) {
        return !stack.isEmpty() && !EnchantNbt.read(stack).isEmpty();
    }

    public void markInitial(int slot, ItemStack stack) {
        if (!stack.isEmpty()) initial[slot] = true;
    }

    public boolean consumeInitial(int slot) {
        boolean was = initial[slot];
        initial[slot] = false;
        return was;
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

    /** Screen closed (also on logout / death): give the enchant item back. The tray stays in the wallet. */
    public void close() {
        if (client || closed) return;
        closed = true;
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

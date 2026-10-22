package com.eisiadev.theavarice.wallet;

import com.eisiadev.theavarice.registry.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WalletMenu extends AbstractContainerMenu {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 232;
    public static final int ROWS_Y = 18;
    public static final int ROW_H = 20;
    public static final int VISIBLE_ROWS = 6;
    public static final int INV_Y = 152;

    public WalletMenu(int id, Inventory inventory) {
        super(MenuRegistry.WALLET.get(), id);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < 27) {
            if (!moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

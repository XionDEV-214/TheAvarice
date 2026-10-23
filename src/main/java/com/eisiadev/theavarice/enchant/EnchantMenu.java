package com.eisiadev.theavarice.enchant;

import com.eisiadev.theavarice.registry.MenuRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EnchantMenu extends AbstractContainerMenu {

    public static final int WIDTH = 216;
    public static final int HEIGHT = 254;
    public static final int TARGET_X = 8;
    public static final int TARGET_Y = 20;
    public static final int ROWS_Y = 44;
    public static final int ROW_H = 20;
    public static final int VISIBLE_ROWS = 5;
    public static final int BUTTONS_Y = ROWS_Y + VISIBLE_ROWS * ROW_H + 3;
    public static final int INV_X = (WIDTH - 162) / 2 + 1;
    public static final int INV_Y = 176;

    public static final int TARGET_SLOT = 0;

    private final Container target = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnchantMenu.this.slotsChanged(this);
        }
    };
    private final Player player;

    public EnchantMenu(int id, Inventory inventory) {
        super(MenuRegistry.ENCHANT.get(), id);
        this.player = inventory.player;

        addSlot(new Slot(target, 0, TARGET_X, TARGET_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isEnchanted(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58));
        }
    }

    public static boolean isEnchanted(ItemStack stack) {
        return !stack.isEmpty() && !EnchantNbt.read(stack).isEmpty();
    }

    public ItemStack getTarget() {
        return target.getItem(0);
    }

    public void setTarget(ItemStack stack) {
        target.setItem(0, stack);
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == TARGET_SLOT) {
            if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (isEnchanted(stack) && !slots.get(TARGET_SLOT).hasItem()) {
            if (!moveItemStackTo(stack, TARGET_SLOT, TARGET_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!moveItemStackTo(stack, 28, 37, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 28, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, target);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}

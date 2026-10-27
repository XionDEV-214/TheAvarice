package com.eisiadev.theavarice.wallet.ui;

import brachy.modularui.screen.ModularContainerMenu;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The wallet's ModularUI container. Lets the server tell "this player has the wallet open", and returns whatever
 * is left in the screen's tray / enchant slot when it closes (vanilla calls {@link #removed} on close, logout and
 * death alike).
 */
public class WalletContainer extends ModularContainerMenu {

    public WalletContainer(int containerId) {
        super(containerId);
    }

    /**
     * Shift-clicking a coin in the coin-out tray must put it in the inventory, not back into the coin-in slot
     * (which has the highest shift-click priority so that shift-clicking coins in the inventory deposits them).
     */
    @Override
    protected ItemStack transferItem(ModularSlot fromSlot, ItemStack fromStack) {
        SlotGroup group = fromSlot.getSlotGroup();
        if (group != null && WalletUI.SLOT_TRAY.equals(group.getName()) && getGuiData() != null) {
            getGuiData().getPlayer().getInventory().add(fromStack);
            return fromStack;
        }
        return super.transferItem(fromSlot, fromStack);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide && getGuiData() instanceof WalletGuiData data) {
            data.session().close();
        }
    }
}

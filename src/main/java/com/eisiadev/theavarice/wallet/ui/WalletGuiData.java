package com.eisiadev.theavarice.wallet.ui;

import com.eisiadev.theavarice.wallet.WalletGroupView;
import brachy.modularui.factory.GuiData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * What the wallet screen is built from on both sides: the currency groups (with the balances at open time) and
 * the title. The server writes this into the open packet, so the client builds exactly the same sync handlers.
 */
public class WalletGuiData extends GuiData {

    private final List<WalletGroupView> groups;
    private final Component title;

    public WalletGuiData(Player player, List<WalletGroupView> groups, Component title) {
        super(player);
        this.groups = List.copyOf(groups);
        this.title = title;
    }

    public List<WalletGroupView> groups() {
        return groups;
    }

    public Component title() {
        return title;
    }
}

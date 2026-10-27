package com.eisiadev.theavarice.wallet.ui;

import com.eisiadev.theavarice.wallet.WalletEntry;
import com.eisiadev.theavarice.wallet.WalletGroupView;
import com.eisiadev.theavarice.wallet.WalletService;
import brachy.modularui.factory.GuiData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What the wallet screen is built from on both sides: the currency groups (with the balances at open time), the
 * title and the auto-deposit setting. The server writes this into the open packet, so the client builds exactly the
 * same sync handlers. Also owns the {@link WalletSession} (tray, coin-in and enchant slots) of this screen.
 */
public class WalletGuiData extends GuiData {

    private final List<WalletGroupView> groups;
    private final Component title;
    private final boolean autoDeposit;
    private final WalletSession session;

    public WalletGuiData(Player player, List<WalletGroupView> groups, Component title, boolean autoDeposit) {
        super(player);
        this.groups = List.copyOf(groups);
        this.title = title;
        this.autoDeposit = autoDeposit;
        this.session = new WalletSession(player, player.level().isClientSide ? clientCurrency(groups) : WalletService::isCurrency);
    }

    public List<WalletGroupView> groups() {
        return groups;
    }

    public Component title() {
        return title;
    }

    public boolean autoDeposit() {
        return autoDeposit;
    }

    public WalletSession session() {
        return session;
    }

    /** The client can't read the server's config, so it recognises currency by the ids it was sent. */
    private static Predicate<ItemStack> clientCurrency(List<WalletGroupView> groups) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (WalletGroupView group : groups) {
            for (WalletEntry entry : group.entries()) ids.add(entry.id());
        }
        return stack -> ids.contains(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}

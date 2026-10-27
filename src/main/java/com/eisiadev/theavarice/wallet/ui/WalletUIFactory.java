package com.eisiadev.theavarice.wallet.ui;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.network.WalletSyncPacket;
import com.eisiadev.theavarice.wallet.ClientWalletState;
import com.eisiadev.theavarice.wallet.WalletCapability;
import com.eisiadev.theavarice.wallet.WalletGroupView;
import com.eisiadev.theavarice.wallet.WalletService;
import brachy.modularui.api.IUIHolder;
import brachy.modularui.factory.AbstractUIFactory;
import brachy.modularui.factory.GuiManager;
import brachy.modularui.screen.ModularContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Opens the wallet as a ModularUI screen. Not bound to a block or item: the player's own wallet capability. */
public final class WalletUIFactory extends AbstractUIFactory<WalletGuiData> {

    public static final WalletUIFactory INSTANCE = new WalletUIFactory();

    private WalletUIFactory() {
        super(new ResourceLocation(TheAvarice.MOD_ID, "wallet"));
    }

    /** Called from common setup on both sides. */
    public static void register() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    public void open(ServerPlayer player, List<WalletGroupView> groups, Component title) {
        boolean auto = WalletCapability.get(player).autoDeposit();
        GuiManager.open(this, new WalletGuiData(player, groups, title, auto), player);
    }

    @Override
    public @NotNull IUIHolder<WalletGuiData> getGuiHolder(WalletGuiData data) {
        return WalletUI.INSTANCE;
    }

    @Override
    public ModularContainerMenu createContainer(int containerId) {
        return new WalletContainer(containerId);
    }

    @Override
    public void writeGuiData(WalletGuiData data, FriendlyByteBuf buffer) {
        buffer.writeComponent(data.title());
        buffer.writeBoolean(data.autoDeposit());
        WalletSyncPacket.writeGroups(buffer, data.groups());
    }

    @Override
    public @NotNull WalletGuiData readGuiData(Player player, FriendlyByteBuf buffer) {
        // A client asking the server to open this factory gets the server's own snapshot, never its own data.
        if (player instanceof ServerPlayer serverPlayer) {
            return new WalletGuiData(serverPlayer, WalletService.snapshot(serverPlayer), WalletService.title(),
                    WalletCapability.get(serverPlayer).autoDeposit());
        }
        Component title = buffer.readComponent();
        boolean auto = buffer.readBoolean();
        List<WalletGroupView> groups = WalletSyncPacket.readGroups(buffer);
        ClientWalletState.set(groups);
        return new WalletGuiData(player, groups, title, auto);
    }
}

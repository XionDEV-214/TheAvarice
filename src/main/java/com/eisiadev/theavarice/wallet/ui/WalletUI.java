package com.eisiadev.theavarice.wallet.ui;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.client.wallet.WalletClientUI;
import com.eisiadev.theavarice.client.wallet.WalletScreen;
import com.eisiadev.theavarice.wallet.WalletAction;
import com.eisiadev.theavarice.wallet.WalletCapability;
import com.eisiadev.theavarice.wallet.WalletEntry;
import com.eisiadev.theavarice.wallet.WalletGroupView;
import com.eisiadev.theavarice.wallet.WalletService;
import brachy.modularui.api.IUIHolder;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;

/**
 * Builds the wallet panel on both sides. The server side only needs the sync handlers and the player inventory;
 * everything that is drawn is built by {@link WalletClientUI} on the client.
 */
public final class WalletUI implements IUIHolder<WalletGuiData> {

    public static final WalletUI INSTANCE = new WalletUI();

    public static final String PANEL_NAME = "avarice_wallet";
    public static final String SYNC_COUNT = "avarice_count";
    public static final String ACTION = "avarice_wallet_action";

    public static final int WIDTH = 196;
    public static final int HEIGHT = 266;
    public static final int INVENTORY_X = (WIDTH - 162) / 2;
    public static final int INVENTORY_Y = 184;

    private WalletUI() {}

    @Override
    public ModularPanel<?> buildUI(WalletGuiData data, PanelSyncManager syncManager, UISettings settings) {
        Player player = data.getPlayer();
        List<WalletGroupView> groups = data.groups();

        // One sync value per tier, numbered in group/tier order. Both sides build them from the same snapshot.
        LongSyncValue[][] counts = new LongSyncValue[groups.size()][];
        int flat = 0;
        for (int g = 0; g < groups.size(); g++) {
            List<WalletEntry> entries = groups.get(g).entries();
            counts[g] = new LongSyncValue[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                ResourceLocation id = entries.get(i).id();
                long initial = entries.get(i).count();
                LongSyncValue value = new LongSyncValue(() -> initial, () -> balance(player, id));
                syncManager.syncValue(SYNC_COUNT, flat++, value);
                counts[g][i] = value;
            }
        }

        syncManager.registerServerSyncedAction(ACTION, buf -> {
            String key = buf.readUtf(256);
            int index = buf.readVarInt();
            WalletAction action = buf.readEnum(WalletAction.class);
            if (player instanceof ServerPlayer serverPlayer) {
                WalletService.handle(serverPlayer, key, index, action);
            }
        });

        ModularPanel<?> panel = new ModularPanel<>(PANEL_NAME);
        panel.size(WIDTH, HEIGHT);

        if (syncManager.isClient()) {
            WalletClientUI.build(panel, data, syncManager, counts);
        } else {
            panel.child(SlotGroupWidget.playerInventory(false).pos(INVENTORY_X, INVENTORY_Y));
        }
        return panel;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ModularScreen createScreen(WalletGuiData data, ModularPanel<?> mainPanel) {
        return new WalletScreen(mainPanel);
    }

    /** Sends a wallet action to the server. Client side only. */
    public static void sendAction(PanelSyncManager syncManager, String groupKey, int index, WalletAction action) {
        Consumer<FriendlyByteBuf> writer = buf -> {
            buf.writeUtf(groupKey, 256);
            buf.writeVarInt(index);
            buf.writeEnum(action);
        };
        syncManager.callSyncedAction(ACTION, writer);
    }

    private static long balance(Player player, ResourceLocation id) {
        return player.getCapability(WalletCapability.WALLET).map(wallet -> wallet.get(id)).orElse(0L);
    }

    public static String owner() {
        return TheAvarice.MOD_ID;
    }
}

package com.eisiadev.theavarice.wallet.ui;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.client.wallet.WalletClientUI;
import com.eisiadev.theavarice.client.wallet.WalletScreen;
import com.eisiadev.theavarice.enchant.EnchantService;
import com.eisiadev.theavarice.wallet.Wallet;
import com.eisiadev.theavarice.wallet.WalletAction;
import com.eisiadev.theavarice.wallet.WalletCapability;
import com.eisiadev.theavarice.wallet.WalletEntry;
import com.eisiadev.theavarice.wallet.WalletGroupView;
import com.eisiadev.theavarice.wallet.WalletService;
import brachy.modularui.api.IUIHolder;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the wallet panel on both sides. The server side only needs the sync handlers, slots and the player
 * inventory; everything that is drawn is built by {@link WalletClientUI} on the client.
 */
public final class WalletUI implements IUIHolder<WalletGuiData> {

    public static final WalletUI INSTANCE = new WalletUI();

    public static final String PANEL_NAME = "avarice_wallet";
    public static final String SYNC_COUNT = "avarice_count";
    public static final String SYNC_AUTO = "avarice_auto";
    public static final String SLOT_TRAY = "avarice_tray";
    public static final String SLOT_DEPOSIT = "avarice_deposit";
    public static final String SLOT_TARGET = "avarice_target";
    public static final String ACTION = "avarice_wallet_action";
    public static final String ACTION_COLLECT = "avarice_collect";
    public static final String ACTION_TOGGLE_AUTO = "avarice_toggle_auto";
    public static final String ACTION_ENCHANT = "avarice_enchant_apply";

    public static final int WIDTH = 196;
    public static final int HEIGHT = 266;
    public static final int INVENTORY_X = (WIDTH - 162) / 2;
    public static final int INVENTORY_Y = 184;

    private static final int MAX_ENCHANT_REQUESTS = 256;

    private WalletUI() {}

    @Override
    public ModularPanel<?> buildUI(WalletGuiData data, PanelSyncManager syncManager, UISettings settings) {
        Player player = data.getPlayer();
        WalletSession session = data.session();
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

        boolean autoInitial = data.autoDeposit();
        BooleanSyncValue auto = new BooleanSyncValue(() -> autoInitial,
                () -> player.getCapability(WalletCapability.WALLET).map(Wallet::autoDeposit).orElse(true));
        syncManager.syncValue(SYNC_AUTO, 0, auto);

        // Coin-out tray (stored in the player's wallet): take only. Shift-click sends coins to the inventory.
        syncManager.registerSlotGroup(new SlotGroup(SLOT_TRAY, 3, 10, true));
        for (int i = 0; i < WalletSession.TRAY_SLOTS; i++) {
            int slot = i;
            syncManager.itemSlot(SLOT_TRAY, i, new ModularSlot(session.tray, i).accessibility(false, true).slotGroup(SLOT_TRAY)
                    .changeListener((stack, onlyAmount, client, init) -> {
                        if (client && init) session.markInitial(slot, stack);
                    }));
        }
        // Coin-in slot: highest shift-click priority, so shift-clicking coins in the inventory deposits them.
        syncManager.registerSlotGroup(new SlotGroup(SLOT_DEPOSIT, 1, 300, true));
        syncManager.itemSlot(SLOT_DEPOSIT, 0, new ModularSlot(session.deposit, 0).slotGroup(SLOT_DEPOSIT));
        // Enchant page item slot (inactive while the wallet page is shown).
        syncManager.registerSlotGroup(new SlotGroup(SLOT_TARGET, 1, 200, true));
        syncManager.itemSlot(SLOT_TARGET, 0, new ModularSlot(session.target, 0).slotGroup(SLOT_TARGET));

        syncManager.registerServerSyncedAction(ACTION, buf -> {
            String key = buf.readUtf(256);
            int index = buf.readVarInt();
            WalletAction action = buf.readEnum(WalletAction.class);
            if (player instanceof ServerPlayer serverPlayer) {
                WalletService.handle(serverPlayer, session, key, index, action);
            }
        });
        syncManager.registerServerSyncedAction(ACTION_COLLECT, buf -> session.collect());
        syncManager.registerServerSyncedAction(ACTION_TOGGLE_AUTO, buf -> {
            Wallet wallet = WalletCapability.get(player);
            wallet.setAutoDeposit(!wallet.autoDeposit());
        });
        syncManager.registerServerSyncedAction(ACTION_ENCHANT, buf -> {
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_ENCHANT_REQUESTS) return;
            List<EnchantService.Request> requests = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                requests.add(new EnchantService.Request(buf.readResourceLocation(), buf.readVarInt()));
            }
            BigInteger expected = new BigInteger(buf.readByteArray(64));
            if (player instanceof ServerPlayer serverPlayer) {
                EnchantService.apply(serverPlayer, session.target, requests, expected);
            }
        });

        ModularPanel<?> panel = new ModularPanel<>(PANEL_NAME);
        panel.size(WIDTH, HEIGHT);

        if (syncManager.isClient()) {
            WalletClientUI.build(panel, data, syncManager, counts, auto);
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
        syncManager.callSyncedAction(ACTION, buf -> {
            buf.writeUtf(groupKey, 256);
            buf.writeVarInt(index);
            buf.writeEnum(action);
        });
    }

    /** Sends an enchant change request to the server. Client side only. */
    public static void sendEnchant(PanelSyncManager syncManager, List<EnchantService.Request> requests, BigInteger expectedNet) {
        syncManager.callSyncedAction(ACTION_ENCHANT, (FriendlyByteBuf buf) -> {
            buf.writeVarInt(requests.size());
            for (EnchantService.Request request : requests) {
                buf.writeResourceLocation(request.id());
                buf.writeVarInt(request.target());
            }
            buf.writeByteArray(expectedNet.toByteArray());
        });
    }

    private static long balance(Player player, ResourceLocation id) {
        return player.getCapability(WalletCapability.WALLET).map(wallet -> wallet.get(id)).orElse(0L);
    }

    public static String owner() {
        return TheAvarice.MOD_ID;
    }
}

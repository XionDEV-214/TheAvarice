package com.eisiadev.theavarice.client.wallet;

import com.eisiadev.theavarice.client.CursorKeeper;
import com.eisiadev.theavarice.client.wallet.widget.BankWidget;
import com.eisiadev.theavarice.client.wallet.widget.CategoryTab;
import com.eisiadev.theavarice.client.wallet.widget.ChuteWidget;
import com.eisiadev.theavarice.client.wallet.widget.Decor;
import com.eisiadev.theavarice.client.wallet.widget.LedDisplay;
import com.eisiadev.theavarice.client.wallet.widget.ModeSegment;
import com.eisiadev.theavarice.client.wallet.widget.PlateButton;
import com.eisiadev.theavarice.client.wallet.widget.RailWidget;
import com.eisiadev.theavarice.client.wallet.widget.ReceiptWidget;
import com.eisiadev.theavarice.client.wallet.widget.RegionArrow;
import com.eisiadev.theavarice.client.wallet.widget.RegionChip;
import com.eisiadev.theavarice.currency.CurrencyGroup;
import com.eisiadev.theavarice.network.SwitchPagePacket;
import com.eisiadev.theavarice.network.WalletNetwork;
import com.eisiadev.theavarice.wallet.ui.WalletGuiData;
import com.eisiadev.theavarice.wallet.ui.WalletUI;
import brachy.modularui.api.MCHelper;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Client half of the wallet UI: every widget that is drawn. Layout constants are in panel coordinates. */
public final class WalletClientUI {

    private static final int W = WalletUI.WIDTH;
    private static final int H = WalletUI.HEIGHT;

    private static final int LED_Y = 21;
    private static final int LED_H = 26;
    private static final int RAIL_Y = 50;
    private static final int BANK_Y = 67;
    private static final int CONTROL_Y = 158;

    private static final int SIDE_W = 80;
    private static final int SIDE_H = 232;
    private static final int SIDE_Y = 16;

    private WalletClientUI() {}

    public static void build(ModularPanel<?> panel, WalletGuiData data, PanelSyncManager sync, LongSyncValue[][] counts) {
        WalletView view = new WalletView(sync, data.title(), data.groups(), counts);
        sync.onClientTick(view::tick);

        panel.disableThemeBackground(true);
        panel.background(PANEL_BACKGROUND);

        // Title strip and the enchant page button
        panel.child(new Decor(view, (g, w, h, now) -> titleStrip(g, w, h, now, view)).pos(4, 4).size(W - 8, 13));
        panel.child(new PlateButton(view, Component.translatable("gui.theavarice.wallet.enchant"), () -> {
            CursorKeeper.remember();
            WalletNetwork.CHANNEL.sendToServer(new SwitchPagePacket(SwitchPagePacket.Page.ENCHANT));
        }).tooltipDynamic(t -> t.clearText()
                        .addLine(Component.translatable("gui.theavarice.wallet.enchant.tooltip"))
                        .addLine(Component.translatable("gui.theavarice.wallet.enchant.tooltip2")
                                .withStyle(net.minecraft.ChatFormatting.GRAY)))
                .pos(W - 62, 3).size(58, 15));

        panel.child(new LedDisplay(view).pos(8, LED_Y).size(W - 16, LED_H));

        // Rail (single-chain categories) or region strip (tokens)
        panel.child(new RailWidget(view).pos(8, RAIL_Y).size(W - 16, 14));
        panel.child(new Decor(view, (g, w, h, now) -> Gfx.well(g, 0, 0, w, h))
                .setEnabledIf(wd -> view.showsRegions()).pos(8, RAIL_Y).size(W - 16, 14));
        panel.child(new RegionArrow(view, true).pos(8, RAIL_Y));
        int chipsLeft = 8 + RegionArrow.W + (W - 16 - 2 * RegionArrow.W - WalletView.REGION_SLOTS * RegionChip.W) / 2;
        for (int slot = 0; slot < WalletView.REGION_SLOTS; slot++) {
            panel.child(new RegionChip(view, slot).pos(chipsLeft + slot * RegionChip.W, RAIL_Y));
        }
        panel.child(new RegionArrow(view, false).pos(W - 8 - RegionArrow.W, RAIL_Y));

        // One tube rack per group; only the current one is enabled.
        for (int g = 0; g < view.groups().size(); g++) {
            panel.child(new BankWidget(view, g).pos(8, BANK_Y));
        }
        panel.child(new Decor(view, (g, w, h, now) -> emptyBank(g, w, h))
                .setEnabledIf(wd -> view.current() < 0).pos(8, BANK_Y).size(BankWidget.WIDTH, BankWidget.HEIGHT));

        // Dispense mode switch
        panel.child(new Decor(view, (g, w, h, now) -> {
            Gfx.text(g, Component.translatable("gui.theavarice.wallet.mode").getString(), 0, 1, 0.5f, Gfx.MUTED, false);
            Gfx.text(g, Component.translatable("gui.theavarice.wallet.mode.hint1").getString(), 0, 7, 0.5f, Gfx.DIM, false);
        }).pos(8, CONTROL_Y + 1).size(44, 12));
        int segX = 54;
        for (WalletView.Mode mode : WalletView.Mode.values()) {
            panel.child(new ModeSegment(view, mode).pos(segX, CONTROL_Y));
            segX += ModeSegment.W + 1;
        }
        panel.child(new Decor(view, (g, w, h, now) -> {
            Gfx.textRight(g, Component.translatable("gui.theavarice.wallet.mode.hint2").getString(), w, 1, 0.5f, Gfx.DIM, false);
            Gfx.textRight(g, Component.translatable("gui.theavarice.wallet.mode.hint3").getString(), w, 7, 0.5f, Gfx.DIM, false);
        }).pos(segX + 2, CONTROL_Y + 1).size(W - 8 - segX - 2, 12));

        // Inventory
        panel.child(new Decor(view, (g, w, h, now) -> {
            Gfx.rect(g, 0, 0, w, 1, Gfx.SEAM);
            Gfx.rect(g, 0, 1, w, 1, 0xFF4A535D);
            Gfx.text(g, Component.translatable("gui.theavarice.wallet.inventory").getString(), 0, 4, 0.5f, Gfx.MUTED, false);
            Gfx.hazard(g, w - 30, 3, 30, 4, Gfx.AMBER, Gfx.HAZARD_DARK, 0);
        }).pos(WalletUI.INVENTORY_X, WalletUI.INVENTORY_Y - 10).size(162, 8));
        panel.child(SlotGroupWidget.playerInventory(false, (index, slot) -> style(slot))
                .pos(WalletUI.INVENTORY_X, WalletUI.INVENTORY_Y));

        // Category tabs on the left edge
        List<CurrencyGroup.Category> categories = view.categories();
        if (categories.size() > 1) {
            for (int i = 0; i < categories.size(); i++) {
                panel.child(new CategoryTab(view, categories.get(i))
                        .pos(-CategoryTab.W + 1, LED_Y + i * (CategoryTab.H + 2)));
            }
        }

        panel.child(sidePanel(view));
    }

    private static ParentWidget<?> sidePanel(WalletView view) {
        SidePanel side = new SidePanel();
        side.pos(W - 3, SIDE_Y).size(SIDE_W, SIDE_H);
        side.excludeAreaInRecipeViewer();

        side.child(new Decor(view, (g, w, h, now) -> banner(g, w, h, "gui.theavarice.wallet.banner.out", Gfx.AMBER, now))
                .pos(8, 6).size(SIDE_W - 16, 10));
        side.child(new ChuteWidget(view).pos((SIDE_W - ChuteWidget.W) / 2, 19));
        side.child(new Decor(view, (g, w, h, now) -> banner(g, w, h, "gui.theavarice.wallet.banner.receipt", Gfx.CYAN, now))
                .pos(8, 95).size(SIDE_W - 16, 10));
        side.child(new ReceiptWidget(view).pos(6, 107).size(SIDE_W - 12, SIDE_H - 113));
        return side;
    }

    /** Fixed side cabinet bolted to the right of the panel. */
    private static final class SidePanel extends ParentWidget<SidePanel> {
        @Override
        public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            GuiGraphics g = context.getGraphics();
            int w = getArea().width;
            int h = getArea().height;
            Gfx.plate(g, 0, 0, w, h);
            Gfx.rivets(g, 0, 0, w, h, 3);
            Gfx.rect(g, 3, 90, w - 6, 1, Gfx.SEAM);
            Gfx.rect(g, 3, 91, w - 6, 1, 0xFF4F5862);
        }
    }

    private static final IDrawable PANEL_BACKGROUND = WalletClientUI::panelBackground;

    private static void panelBackground(GuiContext context, int x, int y, int w, int h, WidgetTheme theme) {
        GuiGraphics g = context.getGraphics();
        Gfx.plate(g, x, y, w, h);
        Gfx.rivets(g, x, y, w, h, 3);
        // Seams between the machine's sections
        int[] seams = {RAIL_Y - 2, BANK_Y - 2, CONTROL_Y - 2};
        for (int seam : seams) {
            Gfx.rect(g, x + 6, y + seam, w - 12, 1, 0x2A000000);
        }
        // Vent slots in the bottom corner
        for (int i = 0; i < 4; i++) {
            Gfx.rect(g, x + w - 26 + i * 5, y + h - 6, 3, 1, 0xFF14181C);
        }
    }

    private static void titleStrip(GuiGraphics g, int w, int h, long now, WalletView view) {
        Gfx.rect(g, 0, 0, w, h, 0xFF15191E);
        Gfx.rect(g, 0, h - 1, w, 1, 0xFF4A535D);
        Gfx.hazard(g, 1, 1, 12, h - 2, Gfx.AMBER, Gfx.HAZARD_DARK, (int) (now / 160));
        float p = Gfx.pulse(now, 2200);
        Gfx.rect(g, 16, 4, 4, 4, Gfx.lerpColor(0xFF0F4A55, Gfx.CYAN, p));
        Gfx.glow(g, 16, 4, 4, 4, Gfx.CYAN, (int) (60 * p));
        String title = Gfx.fit(view.title().getString().toUpperCase(java.util.Locale.ROOT), 90, 1f);
        Gfx.text(g, title, 24, 3, 1f, Gfx.TEXT, true);
    }

    private static void banner(GuiGraphics g, int w, int h, String key, int color, long now) {
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        Gfx.rect(g, 1, 1, w - 2, h - 2, 0xFF15191E);
        Gfx.rect(g, 1, 1, 3, h - 2, color);
        Gfx.rect(g, w - 4, 1, 3, h - 2, color);
        Gfx.textCentered(g, Component.translatable(key).getString(), w / 2f, 3, 0.5f, color, false);
    }

    private static void emptyBank(GuiGraphics g, int w, int h) {
        Gfx.well(g, 0, 0, w, h);
        Gfx.hazard(g, 1, h / 2f - 6, w - 2, 12, 0xFF3A2A00, 0xFF15130C, 0);
        Gfx.textCentered(g, Component.translatable("gui.theavarice.wallet.empty").getString(), w / 2f, h / 2f - 3, 1f, Gfx.AMBER, true);
    }

    private static final IDrawable SLOT_BACKGROUND = (context, x, y, w, h, theme) -> {
        GuiGraphics g = context.getGraphics();
        Gfx.rect(g, x, y, w, h, 0xFF0B0E11);
        Gfx.rect(g, x, y, w, 1, 0xFF050608);
        Gfx.rect(g, x, y, 1, h, 0xFF050608);
        Gfx.rect(g, x + 1, y + h - 1, w - 1, 1, 0xFF3F4750);
        Gfx.rect(g, x + w - 1, y + 1, 1, h - 1, 0xFF3F4750);
        Gfx.rect(g, x + 1, y + 1, w - 2, h - 2, 0xFF181D22);
    };

    /** Dark recessed slot, with the item tooltip workaround from VendingMachine. */
    private static ItemSlot style(ItemSlot slot) {
        slot.disableThemeBackground(true);
        slot.background(SLOT_BACKGROUND);
        RichTooltip tooltip = slot.itemTooltip();
        tooltip.tooltipBuilder(null);
        tooltip.tooltipBuilder(t -> {
            if (!slot.isSynced()) return;
            ItemStack stack = slot.getSlot().getItem();
            if (stack.isEmpty()) return;
            List<Component> lines = MCHelper.getItemToolTip(stack);
            for (int i = 0; i < lines.size(); i++) {
                t.addLine(lines.get(i));
                if (i == 0 && lines.size() > 1) t.spaceLine();
            }
        });
        return slot;
    }
}

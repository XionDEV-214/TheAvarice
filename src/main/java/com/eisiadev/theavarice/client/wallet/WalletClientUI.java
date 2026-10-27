package com.eisiadev.theavarice.client.wallet;

import com.eisiadev.theavarice.client.wallet.enchant.CostPanel;
import com.eisiadev.theavarice.client.wallet.enchant.EnchantRow;
import com.eisiadev.theavarice.client.wallet.enchant.EnchantView;
import com.eisiadev.theavarice.client.wallet.enchant.LevelField;
import com.eisiadev.theavarice.client.wallet.enchant.MaxKey;
import com.eisiadev.theavarice.client.wallet.widget.AutoToggle;
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
import com.eisiadev.theavarice.wallet.ui.WalletGuiData;
import com.eisiadev.theavarice.wallet.ui.WalletSession;
import com.eisiadev.theavarice.wallet.ui.WalletUI;
import brachy.modularui.api.MCHelper;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.TransformWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Client half of the wallet UI: every widget that is drawn. Layout constants are in panel coordinates. */
public final class WalletClientUI {

    private static final int W = WalletUI.WIDTH;
    private static final int H = WalletUI.HEIGHT;
    private static final int PAGE_H = 174;

    private static final int LED_Y = 21;
    private static final int LED_H = 26;
    private static final int RAIL_Y = 50;
    private static final int BANK_Y = 67;
    private static final int CONTROL_Y = 158;

    private static final int ENCHANT_ROWS_Y = 51;

    private static final int SIDE_W = 80;
    private static final int SIDE_H = 232;
    private static final int SIDE_Y = 16;
    private static final int CHUTE_X = (SIDE_W - ChuteWidget.W) / 2;
    private static final int CHUTE_Y = 53;

    private WalletClientUI() {}

    public static void build(ModularPanel<?> panel, WalletGuiData data, PanelSyncManager sync,
                             LongSyncValue[][] counts, BooleanSyncValue auto) {
        WalletView view = new WalletView(sync, data.title(), data.groups(), counts, auto);
        EnchantView enchant = new EnchantView(view, data.session());
        sync.onClientTick(view::tick);
        sync.onClientTick(enchant::tick);

        // The panel itself draws nothing: category tabs go first, then the body covers their inner edge, so tabs
        // always look like they are behind the machine.
        panel.disableThemeBackground(true);
        List<CurrencyGroup.Category> categories = view.categories();
        if (categories.size() > 1) {
            for (int i = 0; i < categories.size(); i++) {
                CategoryTab tab = new CategoryTab(view, categories.get(i));
                tab.setEnabledIf(w -> view.page() == WalletView.Page.WALLET);
                panel.child(tab.pos(-CategoryTab.W + 2, LED_Y + i * (CategoryTab.H + 2)));
            }
        }
        panel.child(new Decor(view, (g, w, h, now) -> body(g, w, h, view)).pos(0, 0).size(W, H));

        // Title strip (the page switch key is added after the pages so it stays on top)
        panel.child(new Decor(view, (g, w, h, now) -> titleStrip(g, w, h, now, view)).pos(4, 4).size(W - 8, 13));

        panel.child(walletPage(view));
        panel.child(enchantPage(view, enchant));
        panel.child(new PlateButton(view, () -> Component.translatable(view.page() == WalletView.Page.WALLET
                ? "gui.theavarice.wallet.enchant" : "gui.theavarice.enchant.back"), view::togglePage)
                .tooltipDynamic(t -> {
                    t.clearText();
                    if (view.page() == WalletView.Page.WALLET) {
                        t.addLine(Component.translatable("gui.theavarice.wallet.enchant.tooltip"));
                        t.addLine(Component.translatable("gui.theavarice.wallet.enchant.tooltip2").withStyle(ChatFormatting.GRAY));
                    } else {
                        t.addLine(Component.translatable("gui.theavarice.enchant.back.tooltip"));
                    }
                })
                .pos(W - 62, 3).size(58, 15));

        // Inventory
        panel.child(new Decor(view, (g, w, h, now) -> {
            Gfx.rect(g, 0, 0, w, 1, Gfx.SEAM);
            Gfx.rect(g, 0, 1, w, 1, 0xFF4A535D);
            Gfx.text(g, Component.translatable("gui.theavarice.wallet.inventory").getString(), 0, 4, 0.5f, Gfx.MUTED, false);
            Gfx.hazard(g, w - 30, 3, 30, 4, Gfx.AMBER, Gfx.HAZARD_DARK, 0);
        }).pos(WalletUI.INVENTORY_X, WalletUI.INVENTORY_Y - 10).size(162, 8));
        panel.child(SlotGroupWidget.playerInventory(false, (index, slot) -> style(slot))
                .pos(WalletUI.INVENTORY_X, WalletUI.INVENTORY_Y));

        panel.child(sidePanel(view, data.session()));
    }

    // ---------- Wallet page ----------

    private static ParentWidget<?> walletPage(WalletView view) {
        Page page = new Page();
        page.setEnabledIf(w -> view.page() == WalletView.Page.WALLET);

        page.child(new LedDisplay(view).pos(8, LED_Y).size(W - 16, LED_H));

        // Rail (single-chain categories) or region strip (tokens)
        page.child(new RailWidget(view).pos(8, RAIL_Y).size(W - 16, 14));
        page.child(new Decor(view, (g, w, h, now) -> Gfx.well(g, 0, 0, w, h))
                .setEnabledIf(wd -> view.showsRegions()).pos(8, RAIL_Y).size(W - 16, 14));
        page.child(new RegionArrow(view, true).pos(8, RAIL_Y));
        int chipsLeft = 8 + RegionArrow.W + (W - 16 - 2 * RegionArrow.W - WalletView.REGION_SLOTS * RegionChip.W) / 2;
        for (int slot = 0; slot < WalletView.REGION_SLOTS; slot++) {
            page.child(new RegionChip(view, slot).pos(chipsLeft + slot * RegionChip.W, RAIL_Y));
        }
        page.child(new RegionArrow(view, false).pos(W - 8 - RegionArrow.W, RAIL_Y));

        // One tube rack per group; only the current one is enabled.
        for (int g = 0; g < view.groups().size(); g++) {
            page.child(new BankWidget(view, g).pos(8, BANK_Y));
        }
        page.child(new Decor(view, (g, w, h, now) -> emptyBank(g, w, h))
                .setEnabledIf(wd -> view.current() < 0).pos(8, BANK_Y).size(BankWidget.WIDTH, BankWidget.HEIGHT));

        // Dispense mode switch
        page.child(new Decor(view, (g, w, h, now) -> {
            Gfx.text(g, Component.translatable("gui.theavarice.wallet.mode").getString(), 0, 1, 0.5f, Gfx.MUTED, false);
            Gfx.text(g, Component.translatable("gui.theavarice.wallet.mode.hint1").getString(), 0, 7, 0.5f, Gfx.DIM, false);
        }).pos(8, CONTROL_Y + 1).size(44, 12));
        int segX = 54;
        for (WalletView.Mode mode : WalletView.Mode.values()) {
            page.child(new ModeSegment(view, mode).pos(segX, CONTROL_Y));
            segX += ModeSegment.W + 1;
        }
        page.child(new Decor(view, (g, w, h, now) -> {
            Gfx.textRight(g, Component.translatable("gui.theavarice.wallet.mode.hint2").getString(), w, 1, 0.5f, Gfx.DIM, false);
            Gfx.textRight(g, Component.translatable("gui.theavarice.wallet.mode.hint3").getString(), w, 7, 0.5f, Gfx.DIM, false);
        }).pos(segX + 2, CONTROL_Y + 1).size(W - 8 - segX - 2, 12));
        return page;
    }

    // ---------- Enchant page ----------

    private static ParentWidget<?> enchantPage(WalletView view, EnchantView enchant) {
        Page page = new Page();
        page.setEnabledIf(w -> view.page() == WalletView.Page.ENCHANT);

        // Item slot
        page.child(new Decor(view, (g, w, h, now) -> {
            Gfx.well(g, 0, 0, w, h);
            Gfx.hazard(g, 1, h - 4, w - 2, 3, Gfx.AMBER, Gfx.HAZARD_DARK, 0);
        }).pos(8, LED_Y).size(22, LED_H));
        page.child(style(new ItemSlot()).syncHandler(WalletUI.SLOT_TARGET, 0).pos(10, LED_Y + 2));

        page.child(new CostPanel(view, enchant).pos(33, LED_Y).size(W - 41, LED_H));

        // Enchantment list
        int listH = EnchantView.VISIBLE_ROWS * EnchantRow.H + 2;
        page.child(new Decor(view, (g, w, h, now) -> {
            Gfx.well(g, 0, 0, w, h);
            if (enchant.rows().isEmpty()) {
                String key = enchant.target().isEmpty() ? "gui.theavarice.enchant.hint.insert" : "gui.theavarice.enchant.hint.no_enchant";
                String text = Gfx.fit(Component.translatable(key).getString(), w - 10, 1f);
                Gfx.textCentered(g, text, w / 2f, h / 2f - 4, 1f, Gfx.MUTED, false);
            }
            int max = enchant.maxScroll();
            if (max > 0) {
                int trackH = h - 4;
                int thumb = Math.max(8, trackH * EnchantView.VISIBLE_ROWS / enchant.rows().size());
                int y = 2 + (trackH - thumb) * enchant.scroll / max;
                Gfx.rect(g, w - 5, 2, 3, trackH, 0xFF07090B);
                Gfx.rect(g, w - 5, y, 3, thumb, Gfx.AMBER);
            }
        }).pos(8, ENCHANT_ROWS_Y - 1).size(W - 16, listH));
        for (int i = 0; i < EnchantView.VISIBLE_ROWS; i++) {
            int y = ENCHANT_ROWS_Y + i * EnchantRow.H;
            page.child(new EnchantRow(view, enchant, i).pos(9, y));
            page.child(new LevelField(enchant, i).pos(9 + EnchantRow.FIELD_X, y + 3).size(EnchantRow.FIELD_W, 11));
            page.child(new MaxKey(view, enchant, i).pos(9 + EnchantRow.MAX_X, y + 2));
        }

        int buttonsY = ENCHANT_ROWS_Y + listH + 4;
        page.child(new PlateButton(view, () -> Component.translatable("gui.theavarice.enchant.reset"), enchant::reset)
                .enabledWhen(enchant::anySelected).accent(Gfx.MUTED)
                .pos(8, buttonsY).size(56, 16));
        page.child(new PlateButton(view, () -> Component.translatable("gui.theavarice.enchant.apply"), enchant::apply)
                .enabledWhen(enchant::canApply).accent(Gfx.LED_ON)
                .tooltipDynamic(t -> {
                    t.clearText();
                    for (Component line : enchant.previewLines()) t.addLine(line);
                })
                .pos(W - 8 - 76, buttonsY).size(76, 16));
        return page;
    }

    /** Container for one page's widgets; covers the area above the inventory. */
    private static final class Page extends ParentWidget<Page> {
        Page() {
            pos(0, 0).size(W, PAGE_H);
        }

        @Override
        public boolean canHover() {
            return false;
        }
    }

    // ---------- Side cabinet: coin in, coin out, receipt ----------

    private static ParentWidget<?> sidePanel(WalletView view, WalletSession session) {
        SidePanel side = new SidePanel();
        side.pos(W - 3, SIDE_Y).size(SIDE_W, SIDE_H);
        side.excludeAreaInRecipeViewer();

        side.child(new Decor(view, (g, w, h, now) -> banner(g, w, h, "gui.theavarice.wallet.banner.in", Gfx.LED_ON))
                .pos(8, 6).size(SIDE_W - 16, 10));
        side.child(new Decor(view, (g, w, h, now) -> coinSlot(g, w, h, now)).pos(7, 17).size(20, 20));
        side.child(style(new ItemSlot()).syncHandler(WalletUI.SLOT_DEPOSIT, 0)
                .tooltipDynamic(t -> {
                    t.addLine(Component.translatable("gui.theavarice.wallet.coin_in").withStyle(ChatFormatting.WHITE));
                    t.addLine(Component.translatable("gui.theavarice.wallet.coin_in.hint").withStyle(ChatFormatting.GRAY));
                })
                .pos(8, 18));
        side.child(new AutoToggle(view).pos(30, 18).size(SIDE_W - 38, 18));

        side.child(new Decor(view, (g, w, h, now) -> banner(g, w, h, "gui.theavarice.wallet.banner.out", Gfx.AMBER, now))
                .pos(8, 41).size(SIDE_W - 16, 10));
        ChuteWidget chute = new ChuteWidget(view, session);
        side.child(chute.pos(CHUTE_X, CHUTE_Y));
        for (int i = 0; i < WalletSession.TRAY_SLOTS; i++) {
            int slot = i;
            ItemSlot item = style(new ItemSlot()).syncHandler(WalletUI.SLOT_TRAY, i);
            item.disableThemeBackground(true);
            item.background(IDrawable.EMPTY);
            item.pos(CHUTE_X + ChuteWidget.slotX(i), CHUTE_Y + ChuteWidget.slotY(i));
            // TransformWidget delegates its area to the slot; it only adds the falling offset.
            side.child(new TransformWidget(item)
                    .transform(stack -> stack.translate(0, chute.dropOffset(slot, Util.getMillis()), slot * 0.5f)));
        }
        side.child(new Decor(view, (g, w, h, now) -> ChuteWidget.drawLip(g, w))
                .pos(CHUTE_X, CHUTE_Y).size(ChuteWidget.W, ChuteWidget.LIP_H));

        side.child(new Decor(view, (g, w, h, now) -> banner(g, w, h, "gui.theavarice.wallet.banner.receipt", Gfx.CYAN, now))
                .pos(8, 121).size(SIDE_W - 16, 10));
        side.child(new ReceiptWidget(view).pos(6, 133).size(SIDE_W - 12, SIDE_H - 139));
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
            for (int seam : new int[]{38, 118}) {
                Gfx.rect(g, 3, seam, w - 6, 1, Gfx.SEAM);
                Gfx.rect(g, 3, seam + 1, w - 6, 1, 0xFF4F5862);
            }
        }
    }

    // ---------- Painters ----------

    private static void body(GuiGraphics g, int w, int h, WalletView view) {
        Gfx.plate(g, 0, 0, w, h);
        Gfx.rivets(g, 0, 0, w, h, 3);
        int[] seams = view.page() == WalletView.Page.WALLET
                ? new int[]{RAIL_Y - 2, BANK_Y - 2, CONTROL_Y - 2}
                : new int[]{ENCHANT_ROWS_Y - 3};
        for (int seam : seams) Gfx.rect(g, 6, seam, w - 12, 1, 0x2A000000);
        for (int i = 0; i < 4; i++) Gfx.rect(g, w - 26 + i * 5, h - 6, 3, 1, 0xFF14181C);
    }

    private static void titleStrip(GuiGraphics g, int w, int h, long now, WalletView view) {
        Gfx.rect(g, 0, 0, w, h, 0xFF15191E);
        Gfx.rect(g, 0, h - 1, w, 1, 0xFF4A535D);
        Gfx.hazard(g, 1, 1, 12, h - 2, Gfx.AMBER, Gfx.HAZARD_DARK, (int) (now / 160));
        float p = Gfx.pulse(now, 2200);
        Gfx.rect(g, 16, 4, 4, 4, Gfx.lerpColor(0xFF0F4A55, Gfx.CYAN, p));
        Gfx.glow(g, 16, 4, 4, 4, Gfx.CYAN, (int) (60 * p));
        Component name = view.page() == WalletView.Page.WALLET ? view.title() : Component.translatable("gui.theavarice.enchant");
        String title = Gfx.fit(name.getString().toUpperCase(java.util.Locale.ROOT), 100, 1f);
        Gfx.text(g, title, 24, 3, 1f, Gfx.TEXT, true);
    }

    private static void banner(GuiGraphics g, int w, int h, String key, int color, long now) {
        banner(g, w, h, key, color);
    }

    private static void banner(GuiGraphics g, int w, int h, String key, int color) {
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        Gfx.rect(g, 1, 1, w - 2, h - 2, 0xFF15191E);
        Gfx.rect(g, 1, 1, 3, h - 2, color);
        Gfx.rect(g, w - 4, 1, 3, h - 2, color);
        Gfx.textCentered(g, Component.translatable(key).getString(), w / 2f, 3, 0.5f, color, false);
    }

    /** Coin slot bezel around the deposit slot, with a pulsing insert arrow. */
    private static void coinSlot(GuiGraphics g, int w, int h, long now) {
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        Gfx.rect(g, 1, 1, w - 2, h - 2, 0xFF2A3036);
        float p = Gfx.pulse(now, 1600);
        Gfx.glow(g, 1, 1, w - 2, h - 2, Gfx.LED_ON, (int) (30 + 40 * p));
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

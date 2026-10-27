package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widget.scroll.HorizontalScrollData;
import brachy.modularui.widgets.ListWidget;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The tube rack for one currency group, cheapest tier on the left. Only the rack of the group on screen is enabled.
 * Sizes are tuned so 8 tiers fill the rack; groups with more tiers keep that size and scroll sideways
 * (mouse wheel or the bar along the bottom).
 */
public class BankWidget extends ParentWidget<BankWidget> {

    public static final int WIDTH = 180;
    public static final int HEIGHT = 88;
    public static final int VISIBLE_TIERS = 8;
    public static final int GAP = 12;
    private static final int INNER = WIDTH - 4;
    private static final int MAX_SLOT = 44;
    private static final int SCROLLBAR = 3;
    private static final int PAD = 6;

    private static final IDrawable SCROLL_THUMB = (context, x, y, w, h, theme) -> {
        GuiGraphics g = context.getGraphics();
        Gfx.rect(g, x, y, w, h, Gfx.AMBER);
        Gfx.rect(g, x, y, w, 1, 0xFFFFD27A);
    };

    private final int group;

    public BankWidget(WalletView view, int group) {
        this.group = group;
        size(WIDTH, HEIGHT);
        setEnabledIf(w -> view.current() == group);

        int n = view.tierCount(group);
        if (n == 0) return;
        int slot = Math.min(MAX_SLOT, INNER / Math.min(n, VISIBLE_TIERS));
        int column = Math.max(10, slot - GAP);
        // PAD on both ends keeps the count labels of the outer tubes inside the scroll area.
        int content = n * slot - GAP + 2 * PAD;
        int visible = Math.min(content, VISIBLE_TIERS * slot - GAP + 2 * PAD);

        ListWidget<IWidget, ?> rack = new ListWidget<>();
        rack.scrollDirection(new HorizontalScrollData(false, SCROLLBAR).texture(SCROLL_THUMB));
        rack.pos((WIDTH - visible) / 2, 0).size(visible, HEIGHT);

        // Display order is cheapest first; tier index 0 is the most valuable.
        rack.child(new ParentWidget<>().size(PAD, HEIGHT));
        for (int p = 0; p < n; p++) {
            int tier = n - 1 - p;
            rack.child(new TubeWidget(view, group, tier, column, slot));
            if (p < n - 1 && view.ratio(group, tier) > 0) {
                rack.child(new ExchangeGap(view, group, tier));
            } else if (p < n - 1) {
                rack.child(new ParentWidget<>().size(GAP, HEIGHT));
            }
        }
        rack.child(new ParentWidget<>().size(PAD, HEIGHT));
        child(rack);
    }

    public int group() {
        return group;
    }

    /** The 12px space between two tubes holding the two exchange buttons for that pair. */
    private static final class ExchangeGap extends ParentWidget<ExchangeGap> {
        ExchangeGap(WalletView view, int group, int boundary) {
            size(GAP, HEIGHT);
            child(new ConverterButton(view, group, boundary, true).pos(1, 22));
            child(new ConverterButton(view, group, boundary, false).pos(1, 34));
        }
    }

    @Override
    public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        GuiGraphics g = context.getGraphics();
        int w = getArea().width;
        int h = getArea().height;
        Gfx.well(g, 0, 0, w, h);
        // Back wall: faint vertical gradient and a shelf under the tubes.
        Gfx.vgrad(g, 1, 1, w - 2, TubeWidget.TUBE_TOP + TubeWidget.TUBE_H, 0xFF12171C, 0xFF0B0E11);
        float shelf = TubeWidget.TUBE_TOP + TubeWidget.TUBE_H;
        Gfx.rect(g, 1, shelf, w - 2, 2, 0xFF2C333B);
        Gfx.rect(g, 1, shelf + 2, w - 2, 1, 0xFF07090B);
        Gfx.scanlines(g, 1, 1, w - 2, shelf - 1, 0x0AFFFFFF);
    }
}

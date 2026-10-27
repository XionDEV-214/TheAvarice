package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.widget.ParentWidget;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The tube rack for one currency group. Only the rack of the group on screen is enabled. Tubes are laid out
 * most valuable first, with exchange buttons on each boundary between two tubes.
 */
public class BankWidget extends ParentWidget<BankWidget> {

    public static final int WIDTH = 180;
    public static final int HEIGHT = 88;
    private static final int MAX_SLOT = 44;

    private final int group;

    public BankWidget(WalletView view, int group) {
        this.group = group;
        size(WIDTH, HEIGHT);
        setEnabledIf(w -> view.current() == group);

        int n = view.tierCount(group);
        if (n == 0) return;
        int slot = Math.min(MAX_SLOT, (WIDTH - 4) / n);
        int column = Math.max(10, slot - 12);
        int offset = (WIDTH - slot * n) / 2;

        for (int i = 0; i < n; i++) {
            int slotX = offset + i * slot;
            child(new TubeWidget(view, group, i, column, slot).pos(slotX + (slot - column) / 2, 0));
            if (i > 0 && view.ratio(group, i) > 0) {
                int bx = slotX - ConverterButton.SIZE / 2;
                child(new ConverterButton(view, group, i, true).pos(bx, 22));
                child(new ConverterButton(view, group, i, false).pos(bx, 34));
            }
        }
    }

    public int group() {
        return group;
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

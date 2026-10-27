package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.math.BigInteger;

/** Info rail under the LED for groups without regions: "1 Gold = 1,000,000 Copper" between hazard caps. */
public class RailWidget extends WalletWidget<RailWidget> {

    public RailWidget(WalletView view) {
        super(view);
        setEnabledIf(w -> !view.showsRegions());
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        Gfx.well(g, 0, 0, w, h);
        Gfx.hazard(g, 1, 1, 10, h - 2, Gfx.AMBER, Gfx.HAZARD_DARK, 0);
        Gfx.hazard(g, w - 11, 1, 10, h - 2, Gfx.AMBER, Gfx.HAZARD_DARK, 0);

        int group = view.current();
        String text;
        if (group < 0) {
            text = Component.translatable("gui.theavarice.wallet.rail.none").getString();
        } else {
            int n = view.tierCount(group);
            if (n <= 1) {
                text = Component.translatable("gui.theavarice.wallet.rail.single", view.name(group, 0)).getString();
            } else {
                BigInteger rate = BigInteger.valueOf(view.value(group, 0))
                        .divide(BigInteger.valueOf(Math.max(1, view.value(group, n - 1))));
                text = "1 " + view.name(group, 0) + "  =  " + view.full(rate) + " " + view.name(group, n - 1);
            }
        }
        float scale = 0.5f;
        String fitted = Gfx.fit(text, w - 28, scale);
        Gfx.textCentered(g, fitted, w / 2f, (h - 4) / 2f, scale, Gfx.MUTED, false);
    }
}

package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.widget.Widget;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

/** Base for the wallet's custom-drawn widgets: draws in local coordinates with the current time. */
public abstract class WalletWidget<W extends WalletWidget<W>> extends Widget<W> {

    protected final WalletView view;

    protected WalletWidget(WalletView view) {
        this.view = view;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        render(context, context.getGraphics(), getArea().width, getArea().height, Util.getMillis());
    }

    protected abstract void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now);
}

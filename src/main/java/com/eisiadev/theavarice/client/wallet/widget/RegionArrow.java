package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/** Scrolls the region strip; shift scrolls a full page. */
public class RegionArrow extends WalletWidget<RegionArrow> implements Interactable {

    public static final int W = 10;

    private final boolean left;

    public RegionArrow(WalletView view, boolean left) {
        super(view);
        this.left = left;
        size(W, RegionChip.H);
        setEnabledIf(w -> view.showsRegions());
    }

    private boolean active() {
        return left ? view.regionScroll > 0 : view.regionScroll < view.maxRegionScroll();
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0 || !active()) return Result.IGNORE;
        int step = Interactable.hasShiftDown() ? WalletView.REGION_SLOTS : 1;
        view.scrollRegions(left ? -step : step);
        return Result.SUCCESS;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        boolean active = active();
        boolean hovered = active && isHovering();
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        Gfx.vgrad(g, 1, 1, w - 2, h - 2, hovered ? 0xFF4A5560 : 0xFF353C45, hovered ? 0xFF323A43 : 0xFF23282E);
        Gfx.arrow(g, w / 2f, h / 2f - 0.5f, left, 3, active ? (hovered ? Gfx.CYAN : Gfx.TEXT) : Gfx.DIM);
    }
}

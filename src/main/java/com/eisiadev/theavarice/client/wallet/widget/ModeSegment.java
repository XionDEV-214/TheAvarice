package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** One position of the dispense-amount selector switch (x1 / stack / all). */
public class ModeSegment extends WalletWidget<ModeSegment> implements Interactable {

    public static final int W = 28;
    public static final int H = 12;

    private final WalletView.Mode mode;

    public ModeSegment(WalletView view, WalletView.Mode mode) {
        super(view);
        this.mode = mode;
        size(W, H);
        tooltipDynamic(t -> t.clearText()
                .addLine(Component.translatable("gui.theavarice.wallet.mode").withStyle(ChatFormatting.WHITE))
                .addLine(Component.translatable(mode.key + ".tooltip").withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0) return Result.IGNORE;
        view.setMode(mode);
        return Result.SUCCESS;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        boolean on = view.mode() == mode;
        boolean hovered = isHovering();
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        if (on) {
            Gfx.vgrad(g, 1, 1, w - 2, h - 2, 0xFF1B2A30, 0xFF13202A);
            Gfx.rect(g, 1, h - 3, w - 2, 2, Gfx.CYAN);
            Gfx.glow(g, 1, h - 3, w - 2, 2, Gfx.CYAN, 70);
        } else {
            Gfx.vgrad(g, 1, 1, w - 2, h - 2, hovered ? 0xFF46505B : 0xFF363D46, hovered ? 0xFF2F363E : 0xFF24292F);
            Gfx.rect(g, 1, 1, w - 2, 1, 0xFF66717D);
        }
        String label = Component.translatable(mode.key).getString();
        Gfx.textCentered(g, Gfx.fit(label, w - 4, 0.5f), w / 2f, on ? 3 : 4, 0.5f, on ? Gfx.CYAN : Gfx.TEXT, false);
    }
}

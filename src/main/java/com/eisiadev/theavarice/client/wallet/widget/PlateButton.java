package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** A raised metal key with an amber accent bar, used for page switches like "Enchant". */
public class PlateButton extends WalletWidget<PlateButton> implements Interactable {

    private final Component label;
    private final Runnable action;
    private boolean pressed;

    public PlateButton(WalletView view, Component label, Runnable action) {
        super(view);
        this.label = label;
        this.action = action;
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0) return Result.IGNORE;
        pressed = true;
        WalletView.click(1.0F);
        action.run();
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseReleased(int button) {
        pressed = false;
        return true;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        boolean hovered = isHovering();
        if (!hovered) pressed = false;
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        if (pressed) {
            Gfx.rect(g, 1, 1, w - 2, h - 2, 0xFF1D2227);
        } else {
            Gfx.vgrad(g, 1, 1, w - 2, h - 2, hovered ? 0xFF56616D : 0xFF444C56, hovered ? 0xFF3A424B : 0xFF2C3239);
            Gfx.rect(g, 1, 1, w - 2, 1, 0xFF7A8591);
            Gfx.rect(g, 1, h - 2, w - 2, 1, 0xFF161A1E);
        }
        Gfx.rect(g, 2, 2, 2, h - 4, Gfx.AMBER);
        if (hovered) Gfx.glow(g, 0, 0, w, h, Gfx.CYAN, 90);
        int dy = pressed ? 1 : 0;
        Gfx.textCentered(g, label.getString(), w / 2f + 2, (h - 8) / 2f + 1 + dy, 1f, hovered ? Gfx.CYAN : Gfx.TEXT, true);
    }
}

package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** A raised metal key with a coloured accent bar: page switch, Reset, Apply... */
public class PlateButton extends WalletWidget<PlateButton> implements Interactable {

    private final Supplier<Component> label;
    private final Runnable action;
    private BooleanSupplier enabled = () -> true;
    private int accent = Gfx.AMBER;
    private boolean pressed;

    public PlateButton(WalletView view, Supplier<Component> label, Runnable action) {
        super(view);
        this.label = label;
        this.action = action;
    }

    public PlateButton enabledWhen(BooleanSupplier enabled) {
        this.enabled = enabled;
        return this;
    }

    public PlateButton accent(int color) {
        this.accent = color;
        return this;
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0) return Result.IGNORE;
        if (!enabled.getAsBoolean()) {
            WalletView.sound(net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 1.6F, 0.3F);
            return Result.SUCCESS;
        }
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
        boolean on = enabled.getAsBoolean();
        boolean hovered = on && isHovering();
        if (!hovered) pressed = false;
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        if (pressed) {
            Gfx.rect(g, 1, 1, w - 2, h - 2, 0xFF1D2227);
        } else if (!on) {
            Gfx.rect(g, 1, 1, w - 2, h - 2, 0xFF22262B);
        } else {
            Gfx.vgrad(g, 1, 1, w - 2, h - 2, hovered ? 0xFF56616D : 0xFF444C56, hovered ? 0xFF3A424B : 0xFF2C3239);
            Gfx.rect(g, 1, 1, w - 2, 1, 0xFF7A8591);
            Gfx.rect(g, 1, h - 2, w - 2, 1, 0xFF161A1E);
        }
        Gfx.rect(g, 2, 2, 2, h - 4, on ? accent : Gfx.DIM);
        if (hovered) Gfx.glow(g, 0, 0, w, h, Gfx.CYAN, 90);
        int dy = pressed ? 1 : 0;
        String text = Gfx.fit(label.get().getString(), w - 10, 1f);
        Gfx.textCentered(g, text, w / 2f + 2, (h - 8) / 2f + 1 + dy, 1f,
                !on ? Gfx.DIM : hovered ? Gfx.CYAN : Gfx.TEXT, on);
    }
}

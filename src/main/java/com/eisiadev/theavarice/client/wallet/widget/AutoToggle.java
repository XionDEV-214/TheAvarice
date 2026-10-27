package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Lever switch for "picked-up currency goes straight into the wallet". */
public class AutoToggle extends WalletWidget<AutoToggle> implements Interactable {

    private long changed;
    private boolean last;

    public AutoToggle(WalletView view) {
        super(view);
        last = view.autoDeposit();
        tooltipDynamic(t -> {
            t.clearText();
            t.addLine(Component.translatable("gui.theavarice.wallet.auto").withStyle(ChatFormatting.WHITE));
            t.addLine(Component.translatable(view.autoDeposit() ? "gui.theavarice.wallet.auto.on" : "gui.theavarice.wallet.auto.off")
                    .withStyle(view.autoDeposit() ? ChatFormatting.GREEN : ChatFormatting.GOLD));
            t.addLine(Component.translatable("gui.theavarice.wallet.auto.hint").withStyle(ChatFormatting.GRAY));
        });
        tooltipAutoUpdate(true);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0) return Result.IGNORE;
        view.toggleAutoDeposit();
        return Result.SUCCESS;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        boolean on = view.autoDeposit();
        if (on != last) {
            last = on;
            changed = now;
        }
        float t = Gfx.easeOutCubic((now - changed) / 160f);
        float knob = on ? t : 1 - t; // 0 = left (off), 1 = right (on)

        Gfx.well(g, 0, 0, w, h);
        Gfx.text(g, Component.translatable("gui.theavarice.wallet.auto.label").getString(), 3, 3, 0.5f, Gfx.MUTED, false);

        // Track
        int tx = 3, ty = 9, tw = w - 6, th = 6;
        Gfx.rect(g, tx, ty, tw, th, 0xFF07090B);
        int fill = on ? Gfx.LED_ON : Gfx.AMBER;
        Gfx.rect(g, tx + 1, ty + 1, (tw - 2) * knob, th - 2, Gfx.withAlpha(fill, 0.55f));
        // Knob
        int kw = 10;
        float kx = tx + (tw - kw) * knob;
        Gfx.rect(g, kx, ty - 2, kw, th + 4, Gfx.FRAME);
        Gfx.vgrad(g, kx + 1, ty - 1, kw - 2, th + 2, 0xFF8D97A3, 0xFF4A535D);
        Gfx.rect(g, kx + 4, ty, 2, th, 0xFF2A3036);

        String state = Component.translatable(on ? "gui.theavarice.wallet.auto.label.on" : "gui.theavarice.wallet.auto.label.off").getString();
        Gfx.textRight(g, state, w - 3, 3, 0.5f, on ? Gfx.LED_ON : Gfx.AMBER, false);
        if (isHovering()) Gfx.outline(g, 0, 0, w, h, Gfx.CYAN);
    }
}

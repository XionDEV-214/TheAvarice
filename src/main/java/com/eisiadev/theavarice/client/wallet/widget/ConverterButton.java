package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Exchange button between two tubes. {@code boundary} is the lower tier's index; "up" turns
 * {@code ratio} of tier {@code boundary} into one of tier {@code boundary - 1} (arrow points left, at the
 * more valuable tube), "down" breaks one of tier {@code boundary - 1} into {@code ratio} of tier {@code boundary}.
 */
public class ConverterButton extends WalletWidget<ConverterButton> implements Interactable {

    public static final int SIZE = 10;

    private final int group;
    private final int boundary;
    private final boolean up;
    private boolean pressed;

    public ConverterButton(WalletView view, int group, int boundary, boolean up) {
        super(view);
        this.group = group;
        this.boundary = boundary;
        this.up = up;
        size(SIZE, SIZE);
        tooltipDynamic(t -> {
            t.clearText();
            long ratio = view.ratio(group, boundary);
            String high = view.name(group, boundary - 1);
            String low = view.name(group, boundary);
            if (up) {
                t.addLine(Component.translatable("gui.theavarice.wallet.convert_up").withStyle(ChatFormatting.WHITE));
                t.addLine(Component.literal(view.full(ratio) + " " + low + "  >  1 " + high).withStyle(ChatFormatting.AQUA));
                long max = ratio > 0 ? view.count(group, boundary) / ratio : 0;
                t.addLine(Component.translatable("gui.theavarice.wallet.convert.max", view.full(max)).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                t.addLine(Component.translatable("gui.theavarice.wallet.convert_down").withStyle(ChatFormatting.WHITE));
                t.addLine(Component.literal("1 " + high + "  >  " + view.full(ratio) + " " + low).withStyle(ChatFormatting.AQUA));
                t.addLine(Component.translatable("gui.theavarice.wallet.convert.max",
                        view.full(view.count(group, boundary - 1))).withStyle(ChatFormatting.DARK_GRAY));
            }
            t.addLine(Component.translatable("gui.theavarice.wallet.bulk").withStyle(ChatFormatting.GRAY));
            if (!enabled()) t.addLine(Component.translatable("gui.theavarice.wallet.unavailable").withStyle(ChatFormatting.RED));
        });
        tooltipAutoUpdate(true);
    }

    private boolean enabled() {
        return up ? view.canConvertUp(group, boundary) : view.canConvertDown(group, boundary - 1);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0) return Result.IGNORE;
        boolean all = Interactable.hasShiftDown();
        if (up) view.convertUp(group, boundary, all);
        else view.convertDown(group, boundary, all);
        pressed = true;
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
        if (hovered) view.hoverLink(group, boundary);
        boolean enabled = enabled();

        // Ratio label sits above the upper button.
        if (up) {
            long ratio = view.ratio(group, boundary);
            Gfx.textCentered(g, "x" + WalletView.compact(ratio), w / 2f, -7, 0.5f, Gfx.MUTED, false);
        }

        int face = !enabled ? 0xFF22262B : pressed ? 0xFF1B2026 : hovered ? 0xFF46505B : 0xFF353C45;
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        Gfx.rect(g, 1, 1, w - 2, h - 2, face);
        if (!pressed) {
            Gfx.rect(g, 1, 1, w - 2, 1, enabled ? 0xFF6A7581 : 0xFF2E3339);
            Gfx.rect(g, 1, h - 2, w - 2, 1, 0xFF15181C);
        }

        int arrow;
        if (!enabled) {
            arrow = Gfx.DIM;
        } else {
            float p = Gfx.pulse(now, 1400);
            arrow = hovered ? Gfx.CYAN : Gfx.lerpColor(Gfx.shade(Gfx.AMBER, 0.75f), Gfx.AMBER, p);
            if (!hovered) Gfx.glow(g, 0, 0, w, h, Gfx.AMBER, (int) (40 + 50 * p));
        }
        float cy = h / 2f - 0.5f + (pressed ? 1 : 0);
        Gfx.arrow(g, w / 2f, cy, up, 3, arrow);
    }
}

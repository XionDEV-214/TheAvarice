package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.math.BigInteger;

/**
 * Green LED counter with the shown group's total in base units. Unlit "8" segments sit behind the digits and the
 * number rolls to its new value after every change. The left block is a status lamp.
 */
public class LedDisplay extends WalletWidget<LedDisplay> {

    private static final float BIG = 1.5f;
    private static final int NUMBER_LEFT = 44;

    public LedDisplay(WalletView view) {
        super(view);
        tooltipDynamic(t -> {
            t.clearText();
            int g = view.current();
            if (g < 0) return;
            t.addLine(Component.translatable("gui.theavarice.wallet.led.total").withStyle(ChatFormatting.GREEN));
            t.addLine(Component.literal(view.full(view.total(g))).withStyle(ChatFormatting.WHITE));
            t.addLine(Component.translatable("gui.theavarice.wallet.led.unit", view.name(g, view.tierCount(g) - 1))
                    .withStyle(ChatFormatting.DARK_GRAY));
        });
        tooltipAutoUpdate(true);
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        Gfx.well(g, 0, 0, w, h);
        Gfx.rect(g, 2, 2, w - 4, h - 4, Gfx.LED_BG);

        int group = view.current();
        drawStatus(g, h, now);
        Gfx.rect(g, NUMBER_LEFT - 4, 4, 1, h - 8, 0xFF143014);

        String label = Component.translatable("gui.theavarice.wallet.led.total").getString();
        Gfx.text(g, label, NUMBER_LEFT, 4, 0.5f, Gfx.LED_LABEL, false);
        if (group >= 0) {
            String right = groupLabel(group);
            Gfx.textRight(g, Gfx.fit(right, w - NUMBER_LEFT - 50, 0.5f), w - 5, 4, 0.5f, Gfx.LED_LABEL, false);
        }

        if (group < 0) {
            String none = Component.translatable("gui.theavarice.wallet.led.none").getString();
            Gfx.textRight(g, none, w - 6, 12, 1f, Gfx.RED, false);
        } else {
            drawNumber(g, view.displayedTotal(group, now), w);
        }

        Gfx.scanlines(g, 2, 2, w - 4, h - 4, 0x22000000);
        Gfx.rect(g, 2, 2, w - 4, 1, 0x18FFFFFF);
    }

    private String groupLabel(int group) {
        var entries = view.group(group).entries();
        String category = Component.translatable("gui.theavarice.wallet.tab."
                + view.group(group).category().name().toLowerCase(java.util.Locale.ROOT)).getString();
        if (view.showsRegions()) return category + " / " + view.name(group, entries.size() - 1);
        return category;
    }

    private void drawStatus(GuiGraphics g, int h, long now) {
        WalletView.Status status = view.status(now);
        int color = switch (status) {
            case READY -> Gfx.LED_ON;
            case DISPENSE, EXCHANGE -> Gfx.AMBER;
            case INCOMING -> Gfx.CYAN;
            case DENIED -> Gfx.RED;
        };
        boolean blink = status != WalletView.Status.READY && (now / 120) % 2 == 0;
        int lamp = blink ? Gfx.shade(color, 0.35f) : color;
        Gfx.rect(g, 5, 5, 5, 5, Gfx.FRAME);
        Gfx.rect(g, 6, 6, 3, 3, lamp);
        if (!blink) Gfx.glow(g, 6, 6, 3, 3, color, 90);

        String key = "gui.theavarice.wallet.status." + status.name().toLowerCase(java.util.Locale.ROOT);
        Gfx.text(g, Component.translatable(key).getString(), 13, 6, 0.5f, color, false);

        // Three activity dots
        for (int i = 0; i < 3; i++) {
            boolean on = status == WalletView.Status.READY || (now / 150 + i) % 3 != 0;
            Gfx.rect(g, 6 + i * 5, h - 8, 3, 3, on ? Gfx.shade(color, 0.8f) : Gfx.LED_GHOST);
        }
    }

    /**
     * Seven-segment style: digit cells right-aligned with a separator after every third digit, lit for the real
     * number and unlit ("8" / ",") for the rest of the window, so grouping always reads the same.
     */
    private void drawNumber(GuiGraphics g, BigInteger total, int w) {
        float right = w - 6;
        float left = NUMBER_LEFT;
        String digits = total.abs().toString();
        float scale = BIG;
        if (measureDigits(digits.length(), scale) > right - left) scale = 1f;
        if (measureDigits(digits.length(), scale) > right - left) {
            String text = WalletView.compact(total);
            Gfx.textRight(g, text, right, 11, BIG, Gfx.LED_ON, false);
            return;
        }
        float y = scale == BIG ? 11 : 13;
        float x = right;
        for (int k = 0; ; k++) {
            if (k > 0 && k % 3 == 0) {
                float cw = cell(',', scale);
                if (x - cw - cell('8', scale) < left) break;
                x -= cw;
                boolean lit = k < digits.length();
                Gfx.text(g, ",", x, y, scale, lit ? Gfx.LED_ON : Gfx.LED_GHOST, false);
            }
            float cw = cell('8', scale);
            if (x - cw < left) break;
            x -= cw;
            Gfx.text(g, "8", x, y, scale, Gfx.LED_GHOST, false);
            if (k < digits.length()) {
                String c = String.valueOf(digits.charAt(digits.length() - 1 - k));
                Gfx.text(g, c, x + 0.5f * scale, y + 0.5f * scale, scale, 0x40205A10, false);
                Gfx.text(g, c, x, y, scale, Gfx.LED_ON, false);
            }
        }
    }

    private static float measureDigits(int count, float scale) {
        return count * cell('8', scale) + Math.max(0, (count - 1) / 3) * cell(',', scale);
    }

    private static float cell(char c, float scale) {
        return (Character.isDigit(c) ? 6 : Gfx.width(String.valueOf(c))) * scale;
    }

}

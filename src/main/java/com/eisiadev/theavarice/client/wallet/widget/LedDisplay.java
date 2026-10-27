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

    private void drawNumber(GuiGraphics g, BigInteger total, int w) {
        float right = w - 6;
        float left = NUMBER_LEFT;
        String text = view.full(total);
        float scale = BIG;
        if (measure(text, scale) > right - left) scale = 1f;
        if (measure(text, scale) > right - left) {
            text = WalletView.compact(total);
            scale = BIG;
        }
        float y = scale == BIG ? 11 : 13;

        // Lit characters right-aligned, then unlit ghost cells continue to the left edge.
        float x = right;
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            float cw = cell(c, scale);
            x -= cw;
            if (Character.isDigit(c)) Gfx.text(g, "8", x, y, scale, Gfx.LED_GHOST, false);
            Gfx.text(g, String.valueOf(c), x + 0.5f * scale, y + 0.5f * scale, scale, 0x40205A10, false);
            Gfx.text(g, String.valueOf(c), x, y, scale, Gfx.LED_ON, false);
        }
        int digits = 0;
        for (int i = 0; i < text.length(); i++) if (Character.isDigit(text.charAt(i))) digits++;
        while (x - cell('8', scale) >= left) {
            if (digits % 3 == 0 && digits > 0 && x - cell(',', scale) - cell('8', scale) >= left && text.indexOf(',') >= 0) {
                x -= cell(',', scale);
                Gfx.text(g, ",", x, y, scale, Gfx.LED_GHOST, false);
            }
            x -= cell('8', scale);
            Gfx.text(g, "8", x, y, scale, Gfx.LED_GHOST, false);
            digits++;
        }
    }

    private static float cell(char c, float scale) {
        return (Character.isDigit(c) ? 6 : Gfx.width(String.valueOf(c))) * scale;
    }

    private static float measure(String text, float scale) {
        float total = 0;
        for (int i = 0; i < text.length(); i++) total += cell(text.charAt(i), scale);
        return total;
    }
}

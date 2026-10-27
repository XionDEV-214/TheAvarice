package com.eisiadev.theavarice.client.wallet.enchant;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import com.eisiadev.theavarice.client.wallet.widget.WalletWidget;
import com.eisiadev.theavarice.enchant.ClientEnchantState;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.List;

/** LED readout on the enchant page: cost (or refund / status) on top, wallet balance below. */
public class CostPanel extends WalletWidget<CostPanel> {

    private final EnchantView enchant;

    public CostPanel(WalletView view, EnchantView enchant) {
        super(view);
        this.enchant = enchant;
        tooltipDynamic(t -> {
            t.clearText();
            for (Component line : enchant.costLines()) t.addLine(line);
        });
        tooltipAutoUpdate(true);
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        Gfx.well(g, 0, 0, w, h);
        Gfx.rect(g, 2, 2, w - 4, h - 4, Gfx.LED_BG);

        EnchantView.Preview preview = enchant.preview();
        int maxW = w - 10;
        int y1 = 4;
        int y2 = 15;

        if (enchant.rules() == null) {
            line(g, Component.translatable("gui.theavarice.enchant.loading"), 5, y1, maxW, Gfx.MUTED);
        } else if (!preview.hasChanges()) {
            line(g, Component.translatable("gui.theavarice.enchant.cost.none"), 5, y1, maxW, Gfx.LED_LABEL);
        } else if (preview.creative()) {
            line(g, Component.translatable("gui.theavarice.enchant.cost.creative"), 5, y1, maxW, Gfx.LED_ON);
        } else {
            BigInteger net = preview.quote().net();
            if (net.signum() > 0) {
                amount(context, g, 5, y1, maxW, Component.translatable("gui.theavarice.enchant.cost.pay"), net,
                        preview.affordable() ? Gfx.AMBER : Gfx.RED);
            } else if (net.signum() < 0) {
                amount(context, g, 5, y1, maxW, Component.translatable("gui.theavarice.enchant.cost.refund"), net.negate(), Gfx.LED_ON);
            } else {
                line(g, Component.translatable("gui.theavarice.enchant.cost.free"), 5, y1, maxW, Gfx.LED_ON);
            }
        }

        Gfx.rect(g, 4, 13, w - 8, 1, 0xFF143014);
        ClientEnchantState.Status status = enchant.status();
        if (status != null) {
            line(g, Component.translatable(status.key()), 5, y2, maxW, status.success() ? Gfx.LED_ON : Gfx.RED);
        } else if (preview.hasChanges() && !preview.affordable()) {
            line(g, Component.translatable("gui.theavarice.enchant.insufficient"), 5, y2, maxW, Gfx.RED);
        } else {
            amount(context, g, 5, y2, maxW, Component.translatable("gui.theavarice.enchant.balance"), preview.budget(), Gfx.LED_LABEL);
        }
        Gfx.scanlines(g, 2, 2, w - 4, h - 4, 0x22000000);
    }

    private static void line(GuiGraphics g, Component text, float x, float y, int maxW, int color) {
        String s = text.getString();
        float scale = Math.min(1f, maxW / (float) Math.max(1, Gfx.width(s)));
        Gfx.text(g, s, x, y + (8 - 8 * scale) / 2f, scale, color, false);
    }

    /** "Cost: [coin] 12 [coin] 3" with coin icons, shrunk to fit. */
    private void amount(ModularGuiContext context, GuiGraphics g, float x, float y, int maxW, Component label,
                        BigInteger value, int color) {
        List<long[]> parts = enchant.breakdown(value);
        String labelText = label.getString();
        float width = Gfx.width(labelText) + 3;
        for (long[] part : parts) width += 9 + 1 + Gfx.width(view.full(part[1])) + 4;
        if (parts.isEmpty()) width += Gfx.width("0");
        float scale = Math.min(1f, maxW / Math.max(1f, width));

        g.pose().pushPose();
        g.pose().translate(x, y + (8 - 8 * scale) / 2f, 0);
        g.pose().scale(scale, scale, 1f);
        Gfx.text(g, labelText, 0, 0, 1f, color, false);
        float cx = Gfx.width(labelText) + 3;
        if (parts.isEmpty()) Gfx.text(g, "0", cx, 0, 1f, color, false);
        for (long[] part : parts) {
            ItemStack coin = enchant.coin((int) part[0]);
            g.flush();
            GuiDraw.drawItem(g, coin, (int) cx, -1, 9, 9, context.getCurrentDrawingZ());
            cx += 10;
            String n = view.full(part[1]);
            Gfx.text(g, n, cx, 0, 1f, color, false);
            cx += Gfx.width(n) + 4;
        }
        g.pose().popPose();
    }
}

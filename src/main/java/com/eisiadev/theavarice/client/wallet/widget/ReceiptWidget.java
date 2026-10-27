package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Thermal-paper receipt of what changed while the wallet was open: withdrawals, exchanges and coins picked up.
 * Built client-side by comparing balances tick to tick, so it needs no extra packets.
 */
public class ReceiptWidget extends WalletWidget<ReceiptWidget> {

    private static final int PAPER = 0xFFE9E4D2;
    private static final int PAPER_EDGE = 0xFFCFC8B0;
    private static final int INK = 0xFF2A2A2A;
    private static final int INK_OUT = 0xFFA8321F;
    private static final int INK_IN = 0xFF2C7A32;
    private static final int INK_EX = 0xFF1E5AA6;
    private static final float SCALE = 0.5f;
    private static final int LINE_H = 6;
    private static final int TOP = 12;

    public ReceiptWidget(WalletView view) {
        super(view);
        tooltipDynamic(t -> {
            t.clearText();
            t.addLine(Component.translatable("gui.theavarice.wallet.receipt").withStyle(ChatFormatting.WHITE));
            int shown = 0;
            for (WalletView.ReceiptLine line : view.receipt()) {
                ChatFormatting color = switch (line.kind()) {
                    case OUT -> ChatFormatting.RED;
                    case IN -> ChatFormatting.GREEN;
                    case EXCHANGE -> ChatFormatting.AQUA;
                };
                t.addLine(Component.literal(String.format("%04d  ", line.serial())).withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(line.text()).withStyle(color)));
                if (++shown >= 12) break;
            }
            if (shown == 0) t.addLine(Component.translatable("gui.theavarice.wallet.receipt.empty").withStyle(ChatFormatting.GRAY));
        });
        tooltipAutoUpdate(true);
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        // Slot the paper comes out of
        Gfx.rect(g, 0, 0, w, 4, Gfx.FRAME);
        Gfx.rect(g, 2, 1, w - 4, 2, 0xFF000000);

        // Paper with a torn (zigzag) bottom edge
        Gfx.rect(g, 3, 3, w - 6, h - 6, PAPER);
        Gfx.rect(g, 3, 3, w - 6, 2, PAPER_EDGE);
        for (int x = 3; x < w - 3; x += 2) {
            Gfx.rect(g, x, h - 3, 1, (x / 2) % 2 == 0 ? 2 : 1, PAPER);
        }
        Gfx.rect(g, w - 3, 4, 1, h - 7, 0x30000000);

        Gfx.textCentered(g, Component.translatable("gui.theavarice.wallet.receipt.header").getString(),
                w / 2f, 6, SCALE, INK, false);
        for (int x = 5; x < w - 5; x += 2) Gfx.rect(g, x, TOP - 2, 1, 1, 0x60000000);

        int maxLines = (h - TOP - 6) / LINE_H;
        int row = 0;
        for (WalletView.ReceiptLine line : view.receipt()) {
            if (row >= maxLines) break;
            float age = Math.min(1, (now - line.time()) / 250f);
            float y = TOP + row * LINE_H - (1 - Gfx.easeOutCubic(age)) * 3;
            int ink = switch (line.kind()) {
                case OUT -> INK_OUT;
                case IN -> INK_IN;
                case EXCHANGE -> INK_EX;
            };
            String serial = String.format("%04d", line.serial() % 10000);
            Gfx.text(g, serial, 5, y, SCALE, Gfx.withAlpha(0xFF8A8576, age), false);
            String text = Gfx.fit(line.text(), w - 24, SCALE);
            Gfx.text(g, text, 18, y, SCALE, Gfx.withAlpha(ink, Math.max(0.15f, age)), false);
            row++;
        }
        if (view.receiptSize() == 0) {
            Gfx.textCentered(g, Component.translatable("gui.theavarice.wallet.receipt.empty").getString(),
                    w / 2f, TOP + 4, SCALE, 0xFF9A9482, false);
        }
    }
}

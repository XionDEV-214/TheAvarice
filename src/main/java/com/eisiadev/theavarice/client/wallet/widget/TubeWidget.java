package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import com.eisiadev.theavarice.wallet.WalletAction;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * One currency tier as a glass coin tube. The stack of discs grows with the order of magnitude of the balance;
 * click to withdraw (left: current mode, right: one, shift: all).
 */
public class TubeWidget extends WalletWidget<TubeWidget> implements Interactable {

    public static final int TUBE_TOP = 6;
    public static final int TUBE_H = 52;
    public static final int ICON_Y = 61;
    public static final int COUNT_Y = 79;
    private static final int CAP = 3;
    private static final int DISC = 3;

    private final int group;
    private final int index;
    private final int tubeW;
    private final int iconSize;
    private final float labelWidth;

    public TubeWidget(WalletView view, int group, int index, int columnWidth, int slotWidth) {
        super(view);
        this.group = group;
        this.index = index;
        this.tubeW = Math.min(20, columnWidth);
        this.iconSize = Math.min(16, columnWidth);
        this.labelWidth = slotWidth - 2;
        size(columnWidth, BankWidget.HEIGHT);
        tooltipDynamic(t -> {
            t.clearText();
            t.addLine(Component.literal(view.name(group, index)).withStyle(ChatFormatting.WHITE));
            t.addLine(Component.translatable("gui.theavarice.wallet.tube.count", view.full(view.count(group, index)))
                    .withStyle(ChatFormatting.GREEN));
            t.addLine(Component.translatable("gui.theavarice.wallet.tube.value", view.full(view.value(group, index)))
                    .withStyle(ChatFormatting.DARK_GRAY));
            t.spaceLine();
            t.addLine(Component.translatable("gui.theavarice.wallet.tube.left",
                    Component.translatable(view.mode().key)).withStyle(ChatFormatting.GRAY));
            t.addLine(Component.translatable("gui.theavarice.wallet.withdraw_one").withStyle(ChatFormatting.GRAY));
            t.addLine(Component.translatable("gui.theavarice.wallet.withdraw_all").withStyle(ChatFormatting.GRAY));
        });
        tooltipAutoUpdate(true);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button == 1) {
            view.withdraw(group, index, WalletAction.WITHDRAW_ONE);
            return Result.SUCCESS;
        }
        if (button == 0) {
            view.withdraw(group, index, Interactable.hasShiftDown() ? WalletAction.WITHDRAW_ALL : view.mode().action);
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        long count = view.count(group, index);
        int color = view.color(group, index);
        boolean hovered = isHovering();
        boolean linked = view.linked(group, index);
        float flash = 1 - Math.min(1, (now - view.flashTime(group, index)) / 350f);

        int x = (w - tubeW) / 2;
        int y = TUBE_TOP;

        // Shadow + glass body
        Gfx.rect(g, x + 1, y + 1, tubeW, TUBE_H, 0x66000000);
        Gfx.rect(g, x, y, tubeW, TUBE_H, Gfx.FRAME);
        Gfx.hgrad(g, x + 1, y + 1, tubeW / 2f - 1, TUBE_H - 2, 0xFF141A20, 0xFF222A33);
        Gfx.hgrad(g, x + tubeW / 2f, y + 1, tubeW / 2f - 1, TUBE_H - 2, 0xFF222A33, 0xFF10151A);

        // Level ticks on the right wall
        for (int tick = 1; tick < 5; tick++) {
            float ty = y + CAP + (TUBE_H - 2 * CAP) * tick / 5f;
            Gfx.rect(g, x + tubeW - 3, ty, 2, 1, 0x30FFFFFF);
        }

        // Coin discs: three per order of magnitude, bottom up.
        int inner = TUBE_H - 2 * CAP - 2;
        int maxDiscs = inner / DISC;
        int discs = count <= 0 ? 0 : Math.min(maxDiscs, (int) Math.ceil(Math.log10(count + 1.0) * 3));
        int discW = tubeW - 4;
        float bottom = y + TUBE_H - CAP - 1;
        for (int d = 0; d < discs; d++) {
            float dy = bottom - (d + 1) * DISC;
            boolean top = d == discs - 1;
            if (top && flash > 0) dy -= flash * 3;
            int body = top ? Gfx.lerpColor(color, 0xFFFFFFFF, flash * 0.6f) : color;
            Gfx.rect(g, x + 3, dy, discW - 2, 1, Gfx.shade(body, 1.25f));
            Gfx.rect(g, x + 2, dy + 1, discW, 1, body);
            Gfx.rect(g, x + 3, dy + 2, discW - 2, 1, Gfx.shade(body, 0.6f));
        }
        if (count <= 0) {
            Gfx.textCentered(g, "0", x + tubeW / 2f, bottom - 6, 0.5f, Gfx.DIM, false);
        }

        // Glass highlight streak
        Gfx.rect(g, x + 2, y + CAP + 1, 1, TUBE_H - 2 * CAP - 2, 0x38FFFFFF);
        Gfx.rect(g, x + 3, y + CAP + 1, 1, TUBE_H - 2 * CAP - 2, 0x14FFFFFF);

        // Metal caps
        Gfx.vgrad(g, x - 1, y - 1, tubeW + 2, CAP + 1, 0xFF7B8592, 0xFF3A4149);
        Gfx.rect(g, x - 1, y - 1, tubeW + 2, 1, 0xFF9AA4AF);
        Gfx.vgrad(g, x - 1, y + TUBE_H - CAP, tubeW + 2, CAP + 1, 0xFF4A525C, 0xFF262B31);

        // Frame highlight
        if (hovered) {
            Gfx.glow(g, x - 1, y - 1, tubeW + 2, TUBE_H + 2, Gfx.CYAN, 140);
            Gfx.outline(g, x - 1, y - 1, tubeW + 2, TUBE_H + 2, Gfx.CYAN);
        } else if (linked) {
            Gfx.outline(g, x - 1, y - 1, tubeW + 2, TUBE_H + 2, Gfx.AMBER);
        }
        if (flash > 0) {
            Gfx.rect(g, x + 1, y + CAP, tubeW - 2, TUBE_H - 2 * CAP, Gfx.withAlpha(0x60FFFFFF, flash));
        }

        // Item plate + count
        int iconX = (w - iconSize) / 2;
        g.flush();
        GuiDraw.drawItem(g, view.stack(group, index), iconX, ICON_Y, iconSize, iconSize, context.getCurrentDrawingZ());
        String text = fittedCount(count);
        Gfx.textCentered(g, text, w / 2f, COUNT_Y, 0.5f, count > 0 ? Gfx.TEXT : Gfx.DIM, false);
    }

    private String fittedCount(long count) {
        String full = view.full(count);
        if (Gfx.width(full) * 0.5f <= labelWidth) return full;
        return WalletView.compact(count);
    }
}

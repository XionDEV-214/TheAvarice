package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Coin-out chute (art borrowed from VendingMachine). Withdrawn coins drop in and bounce; they are already in the
 * inventory, so the pile is only a receipt you can see. Click (or wait a few seconds) to clear it.
 */
public class ChuteWidget extends WalletWidget<ChuteWidget> implements Interactable {

    public static final int W = 40;
    public static final int H = 71;
    private static final int LIP_H = 19;
    private static final int FALL_MS = 900;

    private static final UITexture BACK = UITexture.fullImage(TheAvarice.MOD_ID, "gui/wallet/chute_back");
    private static final UITexture LIP = UITexture.fullImage(TheAvarice.MOD_ID, "gui/wallet/chute_lip");

    public ChuteWidget(WalletView view) {
        super(view);
        size(W, H);
        tooltipDynamic(t -> {
            t.clearText();
            t.addLine(Component.translatable("gui.theavarice.wallet.chute").withStyle(ChatFormatting.WHITE));
            t.addLine(Component.translatable("gui.theavarice.wallet.chute.hint").withStyle(ChatFormatting.GRAY));
        });
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0 || view.drops().isEmpty()) return Result.IGNORE;
        view.collect();
        return Result.SUCCESS;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        BACK.draw(context, 0, 0, w, h);

        long collect = view.collectStart();
        float rise = collect < 0 ? 0 : Gfx.easeOutCubic((now - collect) / (float) WalletView.COLLECT_TIME);
        int floor = h - 18;
        int span = w - 18;

        g.flush();
        for (WalletView.Drop drop : view.drops()) {
            float t = (now - drop.time()) / (float) FALL_MS;
            float y = LIP_H - 16 + (floor - LIP_H + 16) * Gfx.bounceOut(t);
            y -= rise * (y + 24);
            int x = 1 + (int) (((drop.slot() * 0.61803398875) % 1.0) * span);
            int stackShift = (drop.slot() % 3) * 2;
            GuiDraw.drawItem(g, drop.stack(), x, (int) y - stackShift, 16, 16,
                    context.getCurrentDrawingZ() + drop.slot() % 8);
        }
        g.flush();

        // The lip is drawn in front of falling coins.
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        LIP.draw(context, 0, 0, w, LIP_H);
        if (!view.drops().isEmpty() && isHovering()) {
            Gfx.outline(g, 0, 0, w, h, Gfx.AMBER);
        }
        g.pose().popPose();
    }
}

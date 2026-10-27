package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import com.eisiadev.theavarice.wallet.ui.WalletSession;
import com.mojang.blaze3d.systems.RenderSystem;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Coin-out chute. The tray slots (real slots, drawn on top of this widget) hold withdrawn coins until they are
 * taken: click / shift-click a coin, click the chute to collect everything, or wait for the automatic pickup.
 * This widget draws the chute, tracks the tray to animate coins falling in, and fades taken coins upwards.
 */
public class ChuteWidget extends WalletWidget<ChuteWidget> implements Interactable {

    public static final int W = 56;
    public static final int H = 64;
    public static final int LIP_H = 12;
    public static final int COLUMNS = 3;
    private static final int FALL_MS = 700;
    private static final int FADE_MS = 380;

    private static final UITexture BACK = UITexture.fullImage(TheAvarice.MOD_ID, "gui/wallet/chute_back");

    private final WalletSession session;
    private final ItemStack[] last = new ItemStack[WalletSession.TRAY_SLOTS];
    private final long[] dropTime = new long[WalletSession.TRAY_SLOTS];
    private final ItemStack[] ghost = new ItemStack[WalletSession.TRAY_SLOTS];
    private final long[] ghostTime = new long[WalletSession.TRAY_SLOTS];

    public ChuteWidget(WalletView view, WalletSession session) {
        super(view);
        this.session = session;
        size(W, H);
        for (int i = 0; i < last.length; i++) {
            last[i] = ItemStack.EMPTY;
            ghost[i] = ItemStack.EMPTY;
        }
        tooltipDynamic(t -> {
            t.clearText();
            t.addLine(Component.translatable("gui.theavarice.wallet.chute").withStyle(ChatFormatting.WHITE));
            t.addLine(Component.translatable("gui.theavarice.wallet.chute.hint").withStyle(ChatFormatting.GRAY));
            t.addLine(Component.translatable("gui.theavarice.wallet.chute.auto",
                    WalletSession.AUTO_COLLECT_TICKS / 20).withStyle(ChatFormatting.DARK_GRAY));
        });
    }

    /** Resting position of tray slot {@code i} inside the chute: bottom row fills first. */
    public static int slotX(int i) {
        return 1 + (i % COLUMNS) * 18;
    }

    public static int slotY(int i) {
        return H - 2 - 18 - (i / COLUMNS) * 18;
    }

    /** Vertical offset of a coin that has just landed in slot {@code i} (negative while falling). */
    public float dropOffset(int i, long now) {
        long t0 = dropTime[i];
        if (t0 == 0) return 0;
        float t = (now - t0) / (float) FALL_MS;
        if (t >= 1) return 0;
        float distance = slotY(i) + 18 - LIP_H + 4;
        return -distance * (1 - Gfx.bounceOut(t));
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0 || session.trayEmpty()) return Result.IGNORE;
        view.collect();
        return Result.SUCCESS;
    }

    private void track(long now) {
        ItemStack carried = Minecraft.getInstance().player == null ? ItemStack.EMPTY
                : Minecraft.getInstance().player.containerMenu.getCarried();
        for (int i = 0; i < WalletSession.TRAY_SLOTS; i++) {
            ItemStack current = session.tray.getStackInSlot(i);
            ItemStack before = last[i];
            if (before.isEmpty() && !current.isEmpty()) {
                dropTime[i] = now;
            } else if (!before.isEmpty() && current.isEmpty()) {
                // Taken onto the cursor: no effect, the coin is visibly in hand.
                if (!ItemStack.isSameItemSameTags(carried, before)) {
                    ghost[i] = before.copyWithCount(1);
                    ghostTime[i] = now;
                }
            } else if (!before.isEmpty() && current.getCount() < before.getCount()
                    && !ItemStack.isSameItemSameTags(carried, before)) {
                ghost[i] = before.copyWithCount(1);
                ghostTime[i] = now;
            }
            last[i] = current.copy();
        }
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        track(now);

        // Recess and back wall
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        BACK.draw(context, 1, 1, w - 2, h - 2);
        Gfx.rect(g, 1, h - 3, w - 2, 2, 0xFF2C333B);
        if (!session.trayEmpty() && isHovering()) Gfx.outline(g, 0, 0, w, h, Gfx.AMBER);

        // Coins leaving the tray: rise a little and fade out.
        g.flush();
        for (int i = 0; i < WalletSession.TRAY_SLOTS; i++) {
            if (ghost[i].isEmpty()) continue;
            float t = (now - ghostTime[i]) / (float) FADE_MS;
            if (t >= 1) {
                ghost[i] = ItemStack.EMPTY;
                continue;
            }
            float rise = Gfx.easeOutCubic(t) * 6;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1 - t);
            GuiDraw.drawItem(g, ghost[i], slotX(i) + 1, (int) (slotY(i) + 1 - rise), 16, 16, context.getCurrentDrawingZ());
            g.flush();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    /** The chute's front lip, drawn after the tray slots so falling coins pass behind it. */
    public static void drawLip(GuiGraphics g, int w) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        Gfx.rect(g, 0, 0, w, LIP_H, Gfx.FRAME);
        Gfx.vgrad(g, 1, 1, w - 2, LIP_H - 3, 0xFF8C96A2, 0xFF4B545E);
        Gfx.rect(g, 1, 1, w - 2, 1, 0xFFB7C0CA);
        Gfx.rect(g, 4, LIP_H - 5, w - 8, 2, 0xFF0B0D10);
        Gfx.rect(g, 1, LIP_H - 2, w - 2, 1, 0xFF2A3036);
        Gfx.rect(g, 1, LIP_H - 1, w - 2, 1, 0x66000000);
        g.pose().popPose();
    }
}

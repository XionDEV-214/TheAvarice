package com.eisiadev.theavarice.client.wallet.enchant;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import com.eisiadev.theavarice.client.wallet.widget.WalletWidget;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** "Max" key at the end of an enchant row: fills in the highest level the wallet can pay for. */
public class MaxKey extends WalletWidget<MaxKey> implements Interactable {

    private final EnchantView enchant;
    private final int slot;

    public MaxKey(WalletView view, EnchantView enchant, int slot) {
        super(view);
        this.enchant = enchant;
        this.slot = slot;
        size(EnchantRow.MAX_W, EnchantRow.H - 5);
        setEnabledIf(w -> enchant.row(slot) != null);
        tooltipDynamic(t -> {
            t.clearText();
            t.addLine(Component.translatable("gui.theavarice.enchant.max.tooltip"));
            EnchantView.Row row = enchant.row(slot);
            if (row != null && !enchant.upgradable(row)) {
                t.addLine(Component.translatable("gui.theavarice.enchant.max.unavailable").withStyle(ChatFormatting.GRAY));
            }
        });
        tooltipAutoUpdate(true);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        EnchantView.Row row = enchant.row(slot);
        if (button != 0 || row == null) return Result.IGNORE;
        enchant.fillMax(row);
        return Result.SUCCESS;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        EnchantView.Row row = enchant.row(slot);
        if (row == null) return;
        boolean can = enchant.upgradable(row);
        boolean hover = can && isHovering();
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        Gfx.vgrad(g, 1, 1, w - 2, h - 2, !can ? 0xFF22262B : hover ? 0xFF56616D : 0xFF3A424B,
                !can ? 0xFF1A1E22 : hover ? 0xFF3A424B : 0xFF2A3037);
        Gfx.textCentered(g, Component.translatable("gui.theavarice.enchant.max").getString(), w / 2f, (h - 4) / 2f,
                0.5f, !can ? Gfx.DIM : hover ? Gfx.CYAN : Gfx.AMBER, false);
    }
}

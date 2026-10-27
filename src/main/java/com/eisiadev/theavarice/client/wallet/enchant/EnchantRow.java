package com.eisiadev.theavarice.client.wallet.enchant;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import com.eisiadev.theavarice.client.wallet.widget.WalletWidget;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * One visible enchantment row: check box, name and level, arrow. The level field and the Max key are separate
 * widgets on top of it. Click toggles the row.
 */
public class EnchantRow extends WalletWidget<EnchantRow> implements Interactable {

    public static final int W = 172;
    public static final int H = 18;
    public static final int FIELD_X = 108;
    public static final int FIELD_W = 40;
    public static final int MAX_X = 151;
    public static final int MAX_W = 19;

    private final EnchantView enchant;
    private final int slot;

    public EnchantRow(WalletView view, EnchantView enchant, int slot) {
        super(view);
        this.enchant = enchant;
        this.slot = slot;
        size(W, H);
        setEnabledIf(w -> enchant.row(slot) != null);
        tooltipDynamic(t -> {
            t.clearText();
            EnchantView.Row row = enchant.row(slot);
            if (row == null) return;
            for (Component line : enchant.rowLines(row)) t.addLine(line);
        });
        tooltipAutoUpdate(true);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        EnchantView.Row row = enchant.row(slot);
        if (row == null || button != 0) return Result.IGNORE;
        enchant.toggle(row);
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseScrolled(double delta) {
        enchant.scrollBy(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        EnchantView.Row row = enchant.row(slot);
        if (row == null) return;
        int target = enchant.targetOf(row);
        boolean changed = target != row.current;
        boolean hovered = isHovering();

        int bg = changed ? (target > row.current ? 0xFF16301A : 0xFF33270F) : row.selected ? 0xFF222A33 : 0xFF161B21;
        Gfx.rect(g, 0, 0, w, h - 1, hovered ? Gfx.shade(bg, 1.35f) : bg);
        Gfx.rect(g, 0, h - 1, w, 1, 0xFF0A0C0F);
        if (changed) Gfx.rect(g, 0, 0, 2, h - 1, target > row.current ? Gfx.LED_ON : Gfx.AMBER);

        // Check box
        int cy = (h - 9) / 2;
        Gfx.rect(g, 4, cy, 9, 9, Gfx.FRAME);
        Gfx.rect(g, 5, cy + 1, 7, 7, row.selected ? 0xFF1B2A30 : 0xFF2A3036);
        if (row.selected) {
            Gfx.rect(g, 6, cy + 2, 5, 5, Gfx.CYAN);
            Gfx.glow(g, 6, cy + 2, 5, 5, Gfx.CYAN, 50);
        }

        // Name + current level
        String name = Component.translatable(row.enchantment.getDescriptionId()).getString() + " " + row.current;
        int color = row.enchantment.isCurse() ? 0xFFFF7070 : row.current > enchant.naturalMax(row) ? 0xFF7FF4FF : Gfx.TEXT;
        float scale = Math.min(1f, 90f / Math.max(1, Gfx.width(name)));
        Gfx.text(g, name, 16, (h - 8 * scale) / 2f, scale, color, true);

        // Arrow towards the level field
        Gfx.arrow(g, FIELD_X - 4, h / 2f - 0.5f, false, 3, changed ? Gfx.TEXT : Gfx.DIM);
    }
}

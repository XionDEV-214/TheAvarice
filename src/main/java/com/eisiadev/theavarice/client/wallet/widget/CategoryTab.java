package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import com.eisiadev.theavarice.currency.CurrencyGroup;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/** Vertical tab on the left edge of the panel: main currency / AoA coins / AoD tokens. */
public class CategoryTab extends WalletWidget<CategoryTab> implements Interactable {

    public static final int W = 30;
    public static final int H = 24;

    private final CurrencyGroup.Category category;

    public CategoryTab(WalletView view, CurrencyGroup.Category category) {
        super(view);
        this.category = category;
        size(W, H);
        tooltipDynamic(t -> t.clearText().addLine(Component.translatable(
                "gui.theavarice.wallet.tab." + category.name().toLowerCase(Locale.ROOT))));
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0) return Result.IGNORE;
        view.selectCategory(category);
        return Result.SUCCESS;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        // Tabs sit behind the panel (the panel body is drawn after them); the selected one sticks out further.
        boolean active = view.category() == category;
        boolean hovered = isHovering();
        int x0 = active ? 0 : 4;

        Gfx.rect(g, x0, 0, w - x0, h, Gfx.FRAME);
        if (active) {
            Gfx.vgrad(g, x0 + 1, 1, w - x0 - 1, h - 2, Gfx.PLATE_TOP, Gfx.PLATE_BOTTOM);
            Gfx.rect(g, x0 + 1, 1, w - x0 - 1, 1, Gfx.BEVEL_HI);
            Gfx.rect(g, x0 + 1, 1, 2, h - 2, Gfx.CYAN);
            Gfx.glow(g, x0 + 1, 1, 2, h - 2, Gfx.CYAN, 60);
        } else {
            Gfx.vgrad(g, x0 + 1, 1, w - x0 - 1, h - 2, hovered ? 0xFF353C45 : 0xFF262B31, hovered ? 0xFF262B31 : 0xFF1A1E22);
            Gfx.rect(g, x0 + 1, 1, w - x0 - 1, 1, 0xFF444C56);
        }

        List<Integer> groups = view.groupsOf(category);
        if (groups.isEmpty()) return;
        ItemStack icon = view.icon(groups.get(0));
        g.flush();
        GuiDraw.drawItem(g, icon, x0 + (w - 3 - x0 - 16) / 2 + 1, (h - 16) / 2, 16, 16, context.getCurrentDrawingZ());
    }
}

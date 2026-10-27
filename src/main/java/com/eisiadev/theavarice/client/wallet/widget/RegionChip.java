package com.eisiadev.theavarice.client.wallet.widget;

import com.eisiadev.theavarice.client.wallet.Gfx;
import com.eisiadev.theavarice.client.wallet.WalletView;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.screen.viewport.ModularGuiContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** One slot of the token region strip. Shows region {@code regionScroll + slot}. */
public class RegionChip extends WalletWidget<RegionChip> implements Interactable {

    public static final int W = 18;
    public static final int H = 14;

    private final int slot;

    public RegionChip(WalletView view, int slot) {
        super(view);
        this.slot = slot;
        size(W, H);
        setEnabledIf(w -> view.showsRegions() && index() < view.regions().size());
        tooltipDynamic(t -> {
            t.clearText();
            int group = group();
            if (group >= 0) t.addLine(Component.literal(view.name(group, view.tierCount(group) - 1)));
        });
        tooltipAutoUpdate(true);
    }

    private int index() {
        return view.regionScroll + slot;
    }

    private int group() {
        List<Integer> regions = view.regions();
        int index = index();
        return index < regions.size() ? regions.get(index) : -1;
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0 || group() < 0) return Result.IGNORE;
        view.selectRegion(index());
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseScrolled(double delta) {
        view.scrollRegions(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        int group = group();
        if (group < 0) return;
        boolean selected = index() == view.region();
        boolean hovered = isHovering();

        Gfx.rect(g, 0, 0, w, h, selected ? 0xFF2B333C : hovered ? 0xFF1F252C : 0xFF14181D);
        if (selected) {
            Gfx.rect(g, 0, h - 2, w, 2, Gfx.CYAN);
            Gfx.rect(g, 0, 0, w, 1, 0x30FFFFFF);
        } else if (hovered) {
            Gfx.rect(g, 0, h - 1, w, 1, Gfx.shade(Gfx.CYAN, 0.5f));
        }
        ItemStack icon = view.icon(group);
        g.flush();
        GuiDraw.drawItem(g, icon, 3, 1, 12, 12, context.getCurrentDrawingZ());
    }
}

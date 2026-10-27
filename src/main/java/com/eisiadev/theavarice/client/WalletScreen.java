package com.eisiadev.theavarice.client;

import com.eisiadev.theavarice.currency.CurrencyGroup;
import com.eisiadev.theavarice.currency.CurrencyTiers;
import com.eisiadev.theavarice.network.SwitchPagePacket;
import com.eisiadev.theavarice.network.WalletActionPacket;
import com.eisiadev.theavarice.network.WalletNetwork;
import com.eisiadev.theavarice.wallet.ClientWalletState;
import com.eisiadev.theavarice.wallet.WalletAction;
import com.eisiadev.theavarice.wallet.WalletEntry;
import com.eisiadev.theavarice.wallet.WalletGroupView;
import com.eisiadev.theavarice.wallet.WalletMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletScreen extends AbstractContainerScreen<WalletMenu> {

    private static final int ROW_X = 8;
    private static final int ROW_W = 152;
    private static final int ICON_X = 10;
    private static final int TEXT_X = 30;
    private static final int TEXT_W = 90;
    private static final int UP_X = 124;
    private static final int DOWN_X = 142;
    private static final int BUTTON = 16;
    private static final int ZONE_BODY = 0;
    private static final int ZONE_UP = 1;
    private static final int ZONE_DOWN = 2;
    private static final int ENCHANT_W = 44;
    private static final int ENCHANT_H = 12;
    private static final int ENCHANT_X = WalletMenu.WIDTH - 8 - ENCHANT_W;
    private static final int ENCHANT_Y = 4;

    // Category tabs sit on top of the panel, like creative inventory tabs.
    private static final int TAB_X = 4;
    private static final int TAB_W = 26;
    private static final int TAB_H = 22;
    private static final int TAB_GAP = 2;

    // Region sub-tabs (tokens only) take the first row of the list area.
    private static final int REGION_ARROW_W = 10;
    private static final int REGION_SLOT = 18;
    private static final int REGION_VISIBLE = (ROW_W - REGION_ARROW_W * 2 - 2) / REGION_SLOT;
    private static final int REGION_ICONS_X = ROW_X + REGION_ARROW_W + 1;
    private static final int REGION_NONE = -1;
    private static final int REGION_LEFT = -2;
    private static final int REGION_RIGHT = -3;

    private static final int PANEL_COLOR = 0xFFC6C6C6;

    // Remembered for the rest of the session so reopening the wallet returns to the same tab.
    private static CurrencyGroup.Category lastCategory = CurrencyGroup.Category.MAIN;
    private static int lastRegion = 0;

    private final NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
    private int scroll;
    private int regionScroll;
    private boolean tabsShifted;

    public WalletScreen(WalletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WalletMenu.WIDTH;
        this.imageHeight = WalletMenu.HEIGHT;
        this.titleLabelY = 6;
        this.inventoryLabelY = WalletMenu.INV_Y - 10;
    }

    @Override
    protected void init() {
        super.init();
        tabsShifted = false;
        updateTabOffset();
        ensureRegionVisible();
    }

    /** Pushes the panel down by half a tab while tabs are shown, so panel + tabs stay centred. */
    private void updateTabOffset() {
        boolean tabs = showTabs();
        if (tabs != tabsShifted) {
            topPos += tabs ? TAB_H / 2 : -TAB_H / 2;
            tabsShifted = tabs;
        }
    }

    // ---------- Groups / categories ----------

    private List<CurrencyGroup.Category> categories() {
        List<CurrencyGroup.Category> out = new ArrayList<>();
        for (CurrencyGroup.Category category : CurrencyGroup.Category.values()) {
            for (WalletGroupView group : ClientWalletState.groups()) {
                if (group.category() == category) {
                    out.add(category);
                    break;
                }
            }
        }
        return out;
    }

    private boolean showTabs() {
        return categories().size() > 1;
    }

    private CurrencyGroup.Category category() {
        List<CurrencyGroup.Category> categories = categories();
        if (categories.isEmpty() || categories.contains(lastCategory)) return lastCategory;
        return categories.get(0);
    }

    private List<WalletGroupView> groupsOf(CurrencyGroup.Category category) {
        List<WalletGroupView> out = new ArrayList<>();
        for (WalletGroupView group : ClientWalletState.groups()) {
            if (group.category() == category) out.add(group);
        }
        return out;
    }

    private List<WalletGroupView> regions() {
        return groupsOf(CurrencyGroup.Category.TOKENS);
    }

    private int region() {
        return Mth.clamp(lastRegion, 0, Math.max(0, regions().size() - 1));
    }

    private WalletGroupView currentGroup() {
        List<WalletGroupView> groups = groupsOf(category());
        if (groups.isEmpty()) return null;
        return category() == CurrencyGroup.Category.TOKENS ? groups.get(region()) : groups.get(0);
    }

    private List<WalletEntry> entries() {
        WalletGroupView group = currentGroup();
        return group == null ? List.of() : group.entries();
    }

    private boolean hasRegionStrip() {
        return category() == CurrencyGroup.Category.TOKENS && !regions().isEmpty();
    }

    private int rowsTop() {
        return WalletMenu.ROWS_Y + (hasRegionStrip() ? WalletMenu.ROW_H : 0);
    }

    private int visibleRows() {
        return WalletMenu.VISIBLE_ROWS - (hasRegionStrip() ? 1 : 0);
    }

    private int maxScroll() {
        return Math.max(0, entries().size() - visibleRows());
    }

    private int maxRegionScroll() {
        return Math.max(0, regions().size() - REGION_VISIBLE);
    }

    private void ensureRegionVisible() {
        int region = region();
        if (region < regionScroll) regionScroll = region;
        if (region >= regionScroll + REGION_VISIBLE) regionScroll = region - REGION_VISIBLE + 1;
        regionScroll = Mth.clamp(regionScroll, 0, maxRegionScroll());
    }

    // ---------- Hit testing ----------

    private int rowAt(double mouseX, double mouseY) {
        double relX = mouseX - leftPos;
        double relY = mouseY - topPos - rowsTop();
        if (relX < ROW_X || relX >= ROW_X + ROW_W) return -1;
        if (relY < 0 || relY >= visibleRows() * WalletMenu.ROW_H) return -1;
        int visible = (int) (relY / WalletMenu.ROW_H);
        int display = scroll + visible;
        int size = entries().size();
        return display < size ? size - 1 - display : -1;
    }

    private int zoneAt(double mouseX) {
        double relX = mouseX - leftPos;
        if (relX >= UP_X && relX < UP_X + BUTTON) return ZONE_UP;
        if (relX >= DOWN_X && relX < DOWN_X + BUTTON) return ZONE_DOWN;
        return ZONE_BODY;
    }

    private int tabX(int i) {
        return leftPos + TAB_X + i * (TAB_W + TAB_GAP);
    }

    private int tabAt(double mouseX, double mouseY) {
        if (!showTabs()) return -1;
        if (mouseY < topPos - TAB_H + 2 || mouseY >= topPos) return -1;
        List<CurrencyGroup.Category> categories = categories();
        for (int i = 0; i < categories.size(); i++) {
            if (mouseX >= tabX(i) && mouseX < tabX(i) + TAB_W) return i;
        }
        return -1;
    }

    private boolean onRegionStrip(double mouseX, double mouseY) {
        if (!hasRegionStrip()) return false;
        double relX = mouseX - leftPos;
        double relY = mouseY - topPos - WalletMenu.ROWS_Y;
        return relX >= ROW_X && relX < ROW_X + ROW_W && relY >= 0 && relY < WalletMenu.ROW_H - 2;
    }

    /** Region index under the mouse, or one of {@link #REGION_NONE}, {@link #REGION_LEFT}, {@link #REGION_RIGHT}. */
    private int regionAt(double mouseX, double mouseY) {
        if (!onRegionStrip(mouseX, mouseY)) return REGION_NONE;
        double relX = mouseX - leftPos;
        if (relX < ROW_X + REGION_ARROW_W) return REGION_LEFT;
        if (relX >= ROW_X + ROW_W - REGION_ARROW_W) return REGION_RIGHT;
        int slot = (int) ((relX - REGION_ICONS_X) / REGION_SLOT);
        if (relX < REGION_ICONS_X || slot >= REGION_VISIBLE) return REGION_NONE;
        int index = regionScroll + slot;
        return index < regions().size() ? index : REGION_NONE;
    }

    private boolean canConvertUp(int index) {
        List<WalletEntry> list = entries();
        if (index <= 0 || index >= list.size()) return false;
        long ratio = CurrencyTiers.ratio(list.get(index - 1).value(), list.get(index).value());
        return ratio > 0 && list.get(index).count() >= ratio;
    }

    private boolean canConvertDown(int index) {
        List<WalletEntry> list = entries();
        if (index < 0 || index + 1 >= list.size()) return false;
        long ratio = CurrencyTiers.ratio(list.get(index).value(), list.get(index + 1).value());
        return ratio > 0 && list.get(index).count() >= 1;
    }

    private static ItemStack stackOf(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack stackOf(WalletEntry entry) {
        return stackOf(entry.id());
    }

    private ItemStack categoryIcon(CurrencyGroup.Category category) {
        List<WalletGroupView> groups = groupsOf(category);
        return groups.isEmpty() ? ItemStack.EMPTY : stackOf(groups.get(0).icon());
    }

    private static Component categoryName(CurrencyGroup.Category category) {
        return Component.translatable("gui.theavarice.wallet.tab." + category.name().toLowerCase(Locale.ROOT));
    }

    // ---------- Rendering ----------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateTabOffset();
        scroll = Mth.clamp(scroll, 0, maxScroll());
        regionScroll = Mth.clamp(regionScroll, 0, maxRegionScroll());
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderWalletTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        List<CurrencyGroup.Category> categories = showTabs() ? categories() : List.of();
        CurrencyGroup.Category selected = category();
        int hoveredTab = tabAt(mouseX, mouseY);

        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i) != selected) drawTab(graphics, i, categories.get(i), false, hoveredTab == i);
        }

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 2, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL_COLOR);

        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i) == selected) drawTab(graphics, i, categories.get(i), true, false);
        }

        for (Slot slot : menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
            graphics.fill(sx, sy, sx + 17, sy + 17, 0xFFFFFFFF);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
        }

        boolean enchantHovered = onEnchantButton(mouseX, mouseY);
        graphics.fill(x + ENCHANT_X, y + ENCHANT_Y, x + ENCHANT_X + ENCHANT_W, y + ENCHANT_Y + ENCHANT_H, 0xFF000000);
        graphics.fill(x + ENCHANT_X + 1, y + ENCHANT_Y + 1, x + ENCHANT_X + ENCHANT_W - 1, y + ENCHANT_Y + ENCHANT_H - 1,
                enchantHovered ? 0xFFB8B8B8 : 0xFF9C9C9C);
        graphics.drawCenteredString(font, Component.translatable("gui.theavarice.wallet.enchant"),
                x + ENCHANT_X + ENCHANT_W / 2, y + ENCHANT_Y + 2, 0xFFFFFF);

        graphics.fill(x + ROW_X - 1, y + WalletMenu.ROWS_Y - 1,
                x + ROW_X + ROW_W + 1, y + WalletMenu.ROWS_Y + WalletMenu.VISIBLE_ROWS * WalletMenu.ROW_H,
                0xFF373737);

        if (hasRegionStrip()) renderRegionStrip(graphics, mouseX, mouseY);

        List<WalletEntry> list = entries();
        int hoveredRow = rowAt(mouseX, mouseY);
        int hoveredZone = zoneAt(mouseX);
        int rowsTop = rowsTop();
        int visibleRows = visibleRows();

        for (int i = 0; i < visibleRows; i++) {
            int display = scroll + i;
            if (display >= list.size()) break;
            int index = list.size() - 1 - display;

            WalletEntry entry = list.get(index);
            int rowY = y + rowsTop + i * WalletMenu.ROW_H;
            boolean rowHovered = index == hoveredRow;

            graphics.fill(x + ROW_X, rowY, x + ROW_X + ROW_W, rowY + WalletMenu.ROW_H - 2,
                    rowHovered && hoveredZone == ZONE_BODY ? 0xFFA8A8A8 : 0xFF8B8B8B);

            // Goes through the regular item renderer, so model-based effects (e.g. ItemCustomGlitter) show as in inventories.
            ItemStack stack = stackOf(entry);
            if (!stack.isEmpty()) graphics.renderItem(stack, x + ICON_X, rowY + 1);

            String text = format.format(entry.count());
            int width = Math.max(1, font.width(text));
            float scale = width > TEXT_W ? TEXT_W / (float) width : 1.0F;
            graphics.pose().pushPose();
            graphics.pose().translate(x + TEXT_X, rowY + (WalletMenu.ROW_H - 2 - 8 * scale) / 2.0F, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, text, 0, 0, entry.count() > 0 ? 0xFFFFFF : 0xA0A0A0, true);
            graphics.pose().popPose();

            drawButton(graphics, x + UP_X, rowY + 1, "▼", canConvertUp(index),
                    rowHovered && hoveredZone == ZONE_UP);
            drawButton(graphics, x + DOWN_X, rowY + 1, "▲", canConvertDown(index),
                    rowHovered && hoveredZone == ZONE_DOWN);
        }

        if (maxScroll() > 0) {
            int trackX = x + ROW_X + ROW_W + 3;
            int trackTop = y + rowsTop;
            int trackHeight = visibleRows * WalletMenu.ROW_H - 2;
            int thumbHeight = Math.max(8, trackHeight * visibleRows / list.size());
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll();
            graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackHeight, 0xFF373737);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFFFFFFF);
        }
    }

    private void drawTab(GuiGraphics graphics, int i, CurrencyGroup.Category category, boolean selected, boolean hovered) {
        int tx = tabX(i);
        int y = topPos;

        if (selected) {
            // Taller, panel-coloured and open at the bottom so it merges into the panel.
            int ty = y - TAB_H + 2;
            graphics.fill(tx, ty, tx + TAB_W, y + 2, 0xFF000000);
            graphics.fill(tx + 1, ty + 1, tx + TAB_W - 1, y + 2, 0xFFFFFFFF);
            graphics.fill(tx + 2, ty + 2, tx + TAB_W - 1, y + 2, 0xFF555555);
            graphics.fill(tx + 2, ty + 2, tx + TAB_W - 2, y + 3, PANEL_COLOR);
            ItemStack icon = categoryIcon(category);
            if (!icon.isEmpty()) graphics.renderItem(icon, tx + (TAB_W - 16) / 2, ty + 4);
        } else {
            int ty = y - TAB_H + 4;
            graphics.fill(tx, ty, tx + TAB_W, y, 0xFF000000);
            graphics.fill(tx + 1, ty + 1, tx + TAB_W - 1, y, hovered ? 0xFFB8B8B8 : 0xFF9C9C9C);
            ItemStack icon = categoryIcon(category);
            if (!icon.isEmpty()) graphics.renderItem(icon, tx + (TAB_W - 16) / 2, ty + 3);
        }
    }

    private void renderRegionStrip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos;
        int stripY = topPos + WalletMenu.ROWS_Y;
        int stripH = WalletMenu.ROW_H - 2;
        List<WalletGroupView> regions = regions();
        int selected = region();
        int hovered = regionAt(mouseX, mouseY);

        graphics.fill(x + ROW_X, stripY, x + ROW_X + ROW_W, stripY + stripH, 0xFF6E6E6E);

        drawArrow(graphics, x + ROW_X, stripY, stripH, "◀", regionScroll > 0, hovered == REGION_LEFT);
        drawArrow(graphics, x + ROW_X + ROW_W - REGION_ARROW_W, stripY, stripH, "▶",
                regionScroll < maxRegionScroll(), hovered == REGION_RIGHT);

        for (int slot = 0; slot < REGION_VISIBLE; slot++) {
            int index = regionScroll + slot;
            if (index >= regions.size()) break;

            int sx = x + REGION_ICONS_X + slot * REGION_SLOT;
            if (index == selected) {
                graphics.fill(sx, stripY, sx + REGION_SLOT, stripY + stripH, 0xFFFFFFFF);
                graphics.fill(sx + 1, stripY + 1, sx + REGION_SLOT - 1, stripY + stripH - 1, 0xFFA8A8A8);
            } else if (index == hovered) {
                graphics.fill(sx + 1, stripY + 1, sx + REGION_SLOT - 1, stripY + stripH - 1, 0xFF8B8B8B);
            }

            ItemStack icon = stackOf(regions.get(index).icon());
            if (!icon.isEmpty()) graphics.renderItem(icon, sx + 1, stripY + 1);
        }
    }

    private void drawArrow(GuiGraphics graphics, int ax, int ay, int h, String label, boolean enabled, boolean hovered) {
        int face = !enabled ? 0xFF6E6E6E : hovered ? 0xFFB8B8B8 : 0xFF9C9C9C;
        graphics.fill(ax, ay, ax + REGION_ARROW_W, ay + h, 0xFF000000);
        graphics.fill(ax + 1, ay + 1, ax + REGION_ARROW_W - 1, ay + h - 1, face);
        graphics.drawCenteredString(font, label, ax + REGION_ARROW_W / 2 + 1, ay + (h - 8) / 2,
                enabled ? 0xFFFFFF : 0x909090);
    }

    private void drawButton(GuiGraphics graphics, int bx, int by, String label, boolean enabled, boolean hovered) {
        int face = !enabled ? 0xFF6E6E6E : hovered ? 0xFFB8B8B8 : 0xFF9C9C9C;
        graphics.fill(bx, by, bx + BUTTON, by + BUTTON, 0xFF000000);
        graphics.fill(bx + 1, by + 1, bx + BUTTON - 1, by + BUTTON - 1, face);
        graphics.drawCenteredString(font, label, bx + BUTTON / 2, by + 4, enabled ? 0xFFFFFF : 0x909090);
    }

    private boolean onEnchantButton(double mouseX, double mouseY) {
        double relX = mouseX - leftPos;
        double relY = mouseY - topPos;
        return relX >= ENCHANT_X && relX < ENCHANT_X + ENCHANT_W && relY >= ENCHANT_Y && relY < ENCHANT_Y + ENCHANT_H;
    }

    private void renderWalletTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (onEnchantButton(mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.theavarice.wallet.enchant.tooltip"),
                    gray("gui.theavarice.wallet.enchant.tooltip2")), mouseX, mouseY);
            return;
        }

        int tab = tabAt(mouseX, mouseY);
        if (tab >= 0) {
            graphics.renderTooltip(font, categoryName(categories().get(tab)), mouseX, mouseY);
            return;
        }

        int region = regionAt(mouseX, mouseY);
        if (region >= 0) {
            ItemStack icon = stackOf(regions().get(region).icon());
            graphics.renderTooltip(font, icon.isEmpty()
                    ? Component.literal(regions().get(region).key())
                    : icon.getHoverName(), mouseX, mouseY);
            return;
        }

        int index = rowAt(mouseX, mouseY);
        if (index < 0) return;

        WalletEntry entry = entries().get(index);
        List<Component> lines = new ArrayList<>();

        switch (zoneAt(mouseX)) {
            case ZONE_UP -> {
                lines.add(Component.translatable("gui.theavarice.wallet.convert_up"));
                lines.add(gray("gui.theavarice.wallet.bulk"));
                if (!canConvertUp(index)) lines.add(gray("gui.theavarice.wallet.unavailable"));
            }
            case ZONE_DOWN -> {
                lines.add(Component.translatable("gui.theavarice.wallet.convert_down"));
                lines.add(gray("gui.theavarice.wallet.bulk"));
                if (!canConvertDown(index)) lines.add(gray("gui.theavarice.wallet.unavailable"));
            }
            default -> {
                ItemStack stack = stackOf(entry);
                lines.add(stack.isEmpty() ? Component.literal(entry.id().toString()) : stack.getHoverName());
                lines.add(gray("gui.theavarice.wallet.withdraw_stack"));
                lines.add(gray("gui.theavarice.wallet.withdraw_one"));
                lines.add(gray("gui.theavarice.wallet.withdraw_all"));
            }
        }

        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static Component gray(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    // ---------- Input ----------

    private static void playClick() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void scrollRegions(int delta) {
        regionScroll = Mth.clamp(regionScroll + delta, 0, maxRegionScroll());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && onEnchantButton(mouseX, mouseY)) {
            playClick();
            CursorKeeper.remember();
            WalletNetwork.CHANNEL.sendToServer(new SwitchPagePacket(SwitchPagePacket.Page.ENCHANT));
            return true;
        }

        int tab = tabAt(mouseX, mouseY);
        if (tab >= 0) {
            if (button == 0) {
                CurrencyGroup.Category picked = categories().get(tab);
                if (picked != category()) {
                    lastCategory = picked;
                    scroll = 0;
                    ensureRegionVisible();
                    playClick();
                }
            }
            return true;
        }

        if (onRegionStrip(mouseX, mouseY)) {
            if (button == 0) {
                int region = regionAt(mouseX, mouseY);
                if (region == REGION_LEFT && regionScroll > 0) {
                    scrollRegions(hasShiftDown() ? -REGION_VISIBLE : -1);
                    playClick();
                } else if (region == REGION_RIGHT && regionScroll < maxRegionScroll()) {
                    scrollRegions(hasShiftDown() ? REGION_VISIBLE : 1);
                    playClick();
                } else if (region >= 0 && region != region()) {
                    lastRegion = region;
                    scroll = 0;
                    playClick();
                }
            }
            return true;
        }

        int index = rowAt(mouseX, mouseY);
        WalletGroupView group = currentGroup();
        if (index >= 0 && group != null && (button == 0 || button == 1)) {
            boolean shift = hasShiftDown();
            WalletAction action = null;

            switch (zoneAt(mouseX)) {
                case ZONE_UP -> {
                    if (button == 0 && canConvertUp(index)) {
                        action = shift ? WalletAction.CONVERT_UP_ALL : WalletAction.CONVERT_UP;
                    }
                }
                case ZONE_DOWN -> {
                    if (button == 0 && canConvertDown(index)) {
                        action = shift ? WalletAction.CONVERT_DOWN_ALL : WalletAction.CONVERT_DOWN;
                    }
                }
                default -> {
                    if (entries().get(index).count() > 0) {
                        if (button == 1) {
                            action = WalletAction.WITHDRAW_ONE;
                        } else {
                            action = shift ? WalletAction.WITHDRAW_ALL : WalletAction.WITHDRAW_STACK;
                        }
                    }
                }
            }

            if (action != null) {
                WalletNetwork.CHANNEL.sendToServer(new WalletActionPacket(group.key(), index, action));
                playClick();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return super.hasClickedOutside(mouseX, mouseY, left, top, button) && tabAt(mouseX, mouseY) < 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (onRegionStrip(mouseX, mouseY)) {
            if (maxRegionScroll() > 0) scrollRegions(-(int) Math.signum(delta));
            return true;
        }
        if (maxScroll() > 0) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (WalletKeys.OPEN_WALLET.getKey().getValue() == keyCode
                && WalletKeys.OPEN_WALLET.getKeyModifier().isActive(null)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

package com.eisiadev.theavarice.client;

import com.eisiadev.theavarice.currency.WalletMath;
import com.eisiadev.theavarice.enchant.ClientEnchantState;
import com.eisiadev.theavarice.enchant.CostMath;
import com.eisiadev.theavarice.enchant.EnchantMenu;
import com.eisiadev.theavarice.enchant.EnchantNbt;
import com.eisiadev.theavarice.enchant.EnchantRules;
import com.eisiadev.theavarice.enchant.EnchantService;
import com.eisiadev.theavarice.network.EnchantApplyPacket;
import com.eisiadev.theavarice.network.SwitchPagePacket;
import com.eisiadev.theavarice.network.WalletNetwork;
import com.eisiadev.theavarice.wallet.ClientWalletState;
import com.eisiadev.theavarice.wallet.WalletEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EnchantScreen extends AbstractContainerScreen<EnchantMenu> {

    private static final int W = EnchantMenu.WIDTH;

    private static final int BACK_W = 46;
    private static final int BACK_X = W - 8 - BACK_W;
    private static final int BACK_Y = 4;
    private static final int TOP_BTN_H = 12;
    private static final int COST_X = 28;
    private static final int COST_W = W - 8 - COST_X;
    private static final int COST_Y1 = 19;
    private static final int COST_Y2 = 30;

    private static final int ROW_X = 8;
    private static final int ROW_W = 192;
    private static final int CHECK_X = 11;
    private static final int CHECK = 10;
    private static final int NAME_X = 25;
    private static final int NAME_W = 88;
    private static final int ARROW_X = 116;
    private static final int BOX_X = 126;
    private static final int BOX_W = 44;
    private static final int BOX_H = 12;
    private static final int MAX_X = 173;
    private static final int MAX_W = 25;
    private static final int MAX_H = 14;
    private static final int SCROLL_X = ROW_X + ROW_W + 3;

    private static final int RESET_X = 8;
    private static final int RESET_W = 50;
    private static final int APPLY_W = 70;
    private static final int APPLY_X = W - 8 - APPLY_W;
    private static final int BTN_H = 16;

    private static final long STATUS_MILLIS = 4000L;

    private final NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
    private final List<Row> rows = new ArrayList<>();
    private final Map<ResourceLocation, ItemStack> iconCache = new HashMap<>();
    private ItemStack snapshot = ItemStack.EMPTY;
    private int scroll;
    private final int statusSerialAtOpen;

    private static final class Row {
        final ResourceLocation id;
        final Enchantment enchantment;
        final int current;
        final int purchased;
        boolean selected;
        String text = "";
        EditBox box;

        Row(ResourceLocation id, Enchantment enchantment, int current, int purchased) {
            this.id = id;
            this.enchantment = enchantment;
            this.current = current;
            this.purchased = purchased;
        }
    }

    private record Preview(List<Row> changed, List<CostMath.Change> changes, CostMath.Quote quote,
                           BigInteger budget, boolean creative) {
        boolean hasChanges() {
            return !changes.isEmpty();
        }

        boolean affordable() {
            return creative || quote.net().compareTo(budget) <= 0;
        }
    }

    public EnchantScreen(EnchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = EnchantMenu.WIDTH;
        this.imageHeight = EnchantMenu.HEIGHT;
        this.titleLabelY = 6;
        this.inventoryLabelX = EnchantMenu.INV_X;
        this.inventoryLabelY = EnchantMenu.INV_Y - 10;
        this.statusSerialAtOpen = ClientEnchantState.statusSerial();
    }

    @Override
    protected void init() {
        super.init();
        for (Row row : rows) row.box = createBox(row);
        layoutBoxes();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack target = menu.getTarget();
        if (!ItemStack.matches(snapshot, target)) {
            snapshot = target.copy();
            rebuildRows();
        }
        for (Row row : rows) row.box.tick();
    }

    private void rebuildRows() {
        for (Row row : rows) {
            if (getFocused() == row.box) setFocused(null);
            removeWidget(row.box);
        }
        rows.clear();
        scroll = 0;

        if (!snapshot.isEmpty()) {
            Map<ResourceLocation, Integer> purchased = EnchantNbt.readPurchased(snapshot);
            EnchantNbt.read(snapshot).forEach((id, level) -> {
                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
                if (enchantment == null) return;
                Row row = new Row(id, enchantment, level, Math.min(level, purchased.getOrDefault(id, 0)));
                row.box = createBox(row);
                rows.add(row);
            });
        }
        layoutBoxes();
    }

    private EditBox createBox(Row row) {
        EditBox box = new EditBox(font, 0, 0, BOX_W, BOX_H, Component.translatable("gui.theavarice.enchant.target"));
        box.setMaxLength(5);
        box.setFilter(s -> s.chars().allMatch(Character::isDigit));
        box.setValue(row.text);
        box.setResponder(text -> onTextChanged(row, text));
        addRenderableWidget(box);
        return box;
    }

    private void onTextChanged(Row row, String text) {
        if (!text.isEmpty()) {
            int value = parse(text);
            int max = maxTarget(row);
            if (value > max) {
                row.box.setValue(Integer.toString(max));
                return;
            }
            row.selected = true;
        }
        row.text = text;
    }

    private void layoutBoxes() {
        scroll = Mth.clamp(scroll, 0, maxScroll());
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int visible = i - scroll;
            boolean shown = visible >= 0 && visible < EnchantMenu.VISIBLE_ROWS;
            row.box.setVisible(shown);
            if (!shown && getFocused() == row.box) setFocused(null);
            if (shown) {
                row.box.setX(leftPos + BOX_X);
                row.box.setY(topPos + EnchantMenu.ROWS_Y + visible * EnchantMenu.ROW_H + 4);
            }
        }
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - EnchantMenu.VISIBLE_ROWS);
    }

    private static int parse(String text) {
        if (text == null || text.isEmpty()) return -1;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private EnchantRules rules() {
        return ClientEnchantState.rules();
    }

    private boolean creative() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().player.getAbilities().instabuild;
    }

    private boolean upgradable(Row row) {
        EnchantRules rules = rules();
        return rules != null && rules.canUpgrade(row.id, row.enchantment, row.current);
    }

    private int maxTarget(Row row) {
        EnchantRules rules = rules();
        if (rules == null) return row.current;
        return upgradable(row) ? Math.max(row.current, rules.capFor(row.id)) : row.current;
    }

    private int naturalMax(Row row) {
        EnchantRules rules = rules();
        return rules == null ? row.enchantment.getMaxLevel() : rules.naturalMaxFor(row.id, row.enchantment);
    }

    private int targetOf(Row row) {
        if (!row.selected) return row.current;
        int value = parse(row.text);
        if (value < 0) return row.current;
        return Math.min(value, maxTarget(row));
    }

    private List<WalletEntry> wallet() {
        return ClientWalletState.get();
    }

    private BigInteger walletTotal() {
        List<WalletEntry> list = wallet();
        long[] counts = new long[list.size()];
        long[] values = new long[list.size()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = list.get(i).count();
            values[i] = list.get(i).value();
        }
        return WalletMath.total(counts, values);
    }

    private long[] walletValues() {
        List<WalletEntry> list = wallet();
        long[] values = new long[list.size()];
        for (int i = 0; i < values.length; i++) values[i] = list.get(i).value();
        return values;
    }

    private CostMath.Change changeOf(Row row, int target) {
        return new CostMath.Change(rules().curve(row.id, row.enchantment), row.current, row.purchased, target);
    }

    private Preview preview() {
        EnchantRules rules = rules();
        boolean creative = creative();
        List<Row> changed = new ArrayList<>();
        List<CostMath.Change> changes = new ArrayList<>();
        if (rules != null) {
            for (Row row : rows) {
                int target = targetOf(row);
                if (target == row.current) continue;
                changed.add(row);
                changes.add(changeOf(row, target));
            }
        }
        CostMath.Quote quote = rules == null
                ? new CostMath.Quote(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, List.of())
                : CostMath.quote(rules.params(), changes, creative);
        return new Preview(changed, changes, quote, walletTotal(), creative);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderCustomTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 2, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFFC6C6C6);

        for (var slot : menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
            graphics.fill(sx, sy, sx + 17, sy + 17, 0xFFFFFFFF);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
        }

        Preview preview = preview();

        drawButton(graphics, x + BACK_X, y + BACK_Y, BACK_W, TOP_BTN_H,
                Component.translatable("gui.theavarice.enchant.back"), true,
                inside(mouseX, mouseY, BACK_X, BACK_Y, BACK_W, TOP_BTN_H));

        graphics.fill(x + COST_X - 1, y + 17, x + COST_X + COST_W + 1, y + 40, 0xFF373737);
        graphics.fill(x + COST_X, y + 18, x + COST_X + COST_W, y + 39, 0xFF8B8B8B);
        renderCostPanel(graphics, x + COST_X + 2, y, preview);

        graphics.fill(x + ROW_X - 1, y + EnchantMenu.ROWS_Y - 1, x + ROW_X + ROW_W + 1,
                y + EnchantMenu.ROWS_Y + EnchantMenu.VISIBLE_ROWS * EnchantMenu.ROW_H, 0xFF373737);

        if (rows.isEmpty()) {
            Component hint = Component.translatable(menu.getTarget().isEmpty()
                    ? "gui.theavarice.enchant.hint.insert" : "gui.theavarice.enchant.hint.no_enchant");
            graphics.drawCenteredString(font, hint, x + ROW_X + ROW_W / 2,
                    y + EnchantMenu.ROWS_Y + EnchantMenu.VISIBLE_ROWS * EnchantMenu.ROW_H / 2 - 4, 0xA0A0A0);
        }

        for (int i = 0; i < EnchantMenu.VISIBLE_ROWS; i++) {
            int index = scroll + i;
            if (index >= rows.size()) break;
            renderRow(graphics, rows.get(index), x, y + EnchantMenu.ROWS_Y + i * EnchantMenu.ROW_H, mouseX, mouseY);
        }

        if (maxScroll() > 0) {
            int trackTop = y + EnchantMenu.ROWS_Y;
            int trackHeight = EnchantMenu.VISIBLE_ROWS * EnchantMenu.ROW_H - 2;
            int thumbHeight = Math.max(8, trackHeight * EnchantMenu.VISIBLE_ROWS / rows.size());
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll();
            graphics.fill(x + SCROLL_X, trackTop, x + SCROLL_X + 3, trackTop + trackHeight, 0xFF373737);
            graphics.fill(x + SCROLL_X, thumbY, x + SCROLL_X + 3, thumbY + thumbHeight, 0xFFFFFFFF);
        }

        int by = EnchantMenu.BUTTONS_Y;
        boolean anySelected = rows.stream().anyMatch(r -> r.selected || !r.text.isEmpty());
        drawButton(graphics, x + RESET_X, y + by, RESET_W, BTN_H, Component.translatable("gui.theavarice.enchant.reset"),
                anySelected, inside(mouseX, mouseY, RESET_X, by, RESET_W, BTN_H));
        drawButton(graphics, x + APPLY_X, y + by, APPLY_W, BTN_H, Component.translatable("gui.theavarice.enchant.apply"),
                canApply(preview), inside(mouseX, mouseY, APPLY_X, by, APPLY_W, BTN_H));
    }

    private boolean canApply(Preview preview) {
        return rules() != null && preview.hasChanges() && preview.affordable();
    }

    private void renderRow(GuiGraphics graphics, Row row, int x, int rowY, int mouseX, int mouseY) {
        int target = targetOf(row);
        boolean changed = target != row.current;
        int bg = changed ? (target > row.current ? 0xFF7C9C7C : 0xFF9C8A6E) : row.selected ? 0xFF9C9C9C : 0xFF8B8B8B;
        graphics.fill(x + ROW_X, rowY, x + ROW_X + ROW_W, rowY + EnchantMenu.ROW_H - 2, bg);

        int cy = rowY + (EnchantMenu.ROW_H - 2 - CHECK) / 2;
        graphics.fill(x + CHECK_X, cy, x + CHECK_X + CHECK, cy + CHECK, 0xFF000000);
        graphics.fill(x + CHECK_X + 1, cy + 1, x + CHECK_X + CHECK - 1, cy + CHECK - 1,
                row.selected ? 0xFFE0E0E0 : 0xFF555555);
        if (row.selected) {
            graphics.fill(x + CHECK_X + 3, cy + 3, x + CHECK_X + CHECK - 3, cy + CHECK - 3, 0xFF2E7D32);
        }

        MutableComponent name = Component.translatable(row.enchantment.getDescriptionId())
                .append(" " + row.current);
        int color = row.enchantment.isCurse() ? 0xFF8080
                : row.current > naturalMax(row) ? 0x80FFFF : 0xFFFFFF;
        drawScaled(graphics, name, x + NAME_X, rowY + (EnchantMenu.ROW_H - 2) / 2, NAME_W, color);

        graphics.drawString(font, "→", x + ARROW_X, rowY + 5, changed ? 0xFFFFFF : 0x606060, true);

        boolean canMax = upgradable(row);
        drawButton(graphics, x + MAX_X, rowY + 2, MAX_W, MAX_H, Component.translatable("gui.theavarice.enchant.max"),
                canMax, canMax && inside(mouseX, mouseY, MAX_X, rowY - topPos + 2, MAX_W, MAX_H));
    }

    private void renderCostPanel(GuiGraphics graphics, int x, int y, Preview preview) {
        int maxW = COST_W - 4;
        EnchantRules rules = rules();

        if (rules == null) {
            drawScaled(graphics, Component.translatable("gui.theavarice.enchant.loading"), x, y + COST_Y1 + 4, maxW, 0xA0A0A0);
        } else if (!preview.hasChanges()) {
            drawScaled(graphics, Component.translatable("gui.theavarice.enchant.cost.none"), x, y + COST_Y1 + 4, maxW, 0xE0E0E0);
        } else if (preview.creative()) {
            drawScaled(graphics, Component.translatable("gui.theavarice.enchant.cost.creative"), x, y + COST_Y1 + 4, maxW, 0x80FF80);
        } else {
            BigInteger net = preview.quote().net();
            if (net.signum() > 0) {
                drawAmount(graphics, x, y + COST_Y1, maxW, Component.translatable("gui.theavarice.enchant.cost.pay"),
                        net, preview.affordable() ? 0xFFFFFF : 0xFF6060);
            } else if (net.signum() < 0) {
                drawAmount(graphics, x, y + COST_Y1, maxW, Component.translatable("gui.theavarice.enchant.cost.refund"),
                        net.negate(), 0x80FF80);
            } else {
                drawScaled(graphics, Component.translatable("gui.theavarice.enchant.cost.free"), x, y + COST_Y1 + 4, maxW, 0xE0E0E0);
            }
        }

        ClientEnchantState.Status status = ClientEnchantState.status();
        boolean showStatus = status != null && ClientEnchantState.statusSerial() != statusSerialAtOpen
                && System.currentTimeMillis() - status.timeMillis() < STATUS_MILLIS;
        if (showStatus) {
            drawScaled(graphics, Component.translatable(status.key()), x, y + COST_Y2 + 4, maxW,
                    status.success() ? 0x80FF80 : 0xFF6060);
        } else if (preview.hasChanges() && !preview.affordable()) {
            drawScaled(graphics, Component.translatable("gui.theavarice.enchant.insufficient"), x, y + COST_Y2 + 4, maxW, 0xFF6060);
        } else {
            drawAmount(graphics, x, y + COST_Y2, maxW, Component.translatable("gui.theavarice.enchant.balance"),
                    preview.budget(), 0xD0D0D0);
        }
    }

    private void drawAmount(GuiGraphics graphics, int x, int y, int maxW, Component label, BigInteger amount, int color) {
        List<WalletEntry> entries = wallet();
        BigInteger[] parts = WalletMath.breakdown(walletValues(), amount);

        final float icon = 9.0F;
        float width = font.width(label) + 3;
        List<String> texts = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].signum() == 0) continue;
            String text = format.format(parts[i]);
            texts.add(text);
            icons.add(iconOf(entries.get(i).id()));
            width += icon + 1 + font.width(text) + 4;
        }
        if (texts.isEmpty()) {
            texts.add("0");
            icons.add(ItemStack.EMPTY);
            width += font.width("0");
        }

        float scale = Math.min(1.0F, maxW / Math.max(1.0F, width));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + (10 - 8 * scale) / 2.0F, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        int cx = graphics.drawString(font, label, 0, 0, color, true) + 3;
        for (int i = 0; i < texts.size(); i++) {
            ItemStack stack = icons.get(i);
            if (!stack.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(cx, -0.5F, 0);
                graphics.pose().scale(icon / 16.0F, icon / 16.0F, 1.0F);
                graphics.renderItem(stack, 0, 0);
                graphics.pose().popPose();
                cx += (int) icon + 1;
            }
            cx = graphics.drawString(font, texts.get(i), cx, 0, color, true) + 4;
        }
        graphics.pose().popPose();
    }

    private void drawScaled(GuiGraphics graphics, Component text, int x, int cy, int maxW, int color) {
        int width = Math.max(1, font.width(text));
        float scale = width > maxW ? maxW / (float) width : 1.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x, cy - 4 * scale, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }

    private void drawButton(GuiGraphics graphics, int bx, int by, int w, int h, Component label, boolean enabled, boolean hovered) {
        int face = !enabled ? 0xFF6E6E6E : hovered ? 0xFFB8B8B8 : 0xFF9C9C9C;
        graphics.fill(bx, by, bx + w, by + h, 0xFF000000);
        graphics.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, face);
        int textW = font.width(label);
        float scale = textW > w - 4 ? (w - 4) / (float) textW : 1.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(bx + w / 2.0F, by + (h - 8 * scale) / 2.0F, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, label, -textW / 2, 0, enabled ? 0xFFFFFF : 0x909090, true);
        graphics.pose().popPose();
    }

    private ItemStack iconOf(ResourceLocation id) {
        return iconCache.computeIfAbsent(id, key -> {
            Item item = ForgeRegistries.ITEMS.getValue(key);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        });
    }

    private boolean inside(double mouseX, double mouseY, int rx, int ry, int w, int h) {
        double relX = mouseX - leftPos;
        double relY = mouseY - topPos;
        return relX >= rx && relX < rx + w && relY >= ry && relY < ry + h;
    }

    private int rowAt(double mouseX, double mouseY) {
        double relX = mouseX - leftPos;
        double relY = mouseY - topPos - EnchantMenu.ROWS_Y;
        if (relX < ROW_X || relX >= ROW_X + ROW_W) return -1;
        if (relY < 0 || relY >= EnchantMenu.VISIBLE_ROWS * EnchantMenu.ROW_H) return -1;
        int index = scroll + (int) (relY / EnchantMenu.ROW_H);
        return index < rows.size() ? index : -1;
    }

    private int rowTop(int index) {
        return EnchantMenu.ROWS_Y + (index - scroll) * EnchantMenu.ROW_H;
    }

    private void renderCustomTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot != null && hoveredSlot.hasItem()) return;

        if (inside(mouseX, mouseY, APPLY_X, EnchantMenu.BUTTONS_Y, APPLY_W, BTN_H)) {
            graphics.renderComponentTooltip(font, previewLines(preview()), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, BACK_X, BACK_Y, BACK_W, TOP_BTN_H)) {
            graphics.renderTooltip(font, Component.translatable("gui.theavarice.enchant.back.tooltip"), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, COST_X, 18, COST_W, 21)) {
            graphics.renderComponentTooltip(font, costPanelLines(preview()), mouseX, mouseY);
            return;
        }

        int index = rowAt(mouseX, mouseY);
        if (index < 0) return;
        Row row = rows.get(index);
        int top = rowTop(index);
        if (inside(mouseX, mouseY, MAX_X, top + 2, MAX_W, MAX_H)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.theavarice.enchant.max.tooltip"));
            if (!upgradable(row)) lines.add(gray("gui.theavarice.enchant.max.unavailable"));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (inside(mouseX, mouseY, BOX_X, top + 4, BOX_W, BOX_H)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.theavarice.enchant.box.tooltip", maxTarget(row)),
                    gray("gui.theavarice.enchant.box.wheel")), mouseX, mouseY);
        } else if (mouseX - leftPos < ARROW_X) {
            graphics.renderComponentTooltip(font, rowLines(row), mouseX, mouseY);
        }
    }

    private List<Component> rowLines(Row row) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(row.enchantment.getDescriptionId())
                .withStyle(row.enchantment.isCurse() ? ChatFormatting.RED : ChatFormatting.WHITE));
        lines.add(Component.translatable("gui.theavarice.enchant.row.level", row.current,
                row.purchased, row.current - row.purchased).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.theavarice.enchant.row.vanilla_max", naturalMax(row))
                .withStyle(ChatFormatting.GRAY));
        EnchantRules rules = rules();
        if (rules != null) {
            int cap = rules.capFor(row.id);
            if (row.enchantment.isCurse()) {
                lines.add(Component.translatable("gui.theavarice.enchant.row.curse").withStyle(ChatFormatting.RED));
            } else if (cap < rules.params().globalMaxLevel()) {
                lines.add(Component.translatable("gui.theavarice.enchant.row.capped", cap).withStyle(ChatFormatting.GOLD));
            } else {
                lines.add(Component.translatable("gui.theavarice.enchant.row.cap", cap).withStyle(ChatFormatting.GRAY));
            }
            if (!row.enchantment.isCurse() && row.current >= cap) {
                lines.add(gray("gui.theavarice.enchant.row.at_cap"));
            }
        }
        lines.add(gray("gui.theavarice.enchant.row.toggle"));
        return lines;
    }

    private List<Component> previewLines(Preview preview) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.theavarice.enchant.preview"));
        if (!preview.hasChanges()) {
            lines.add(gray("gui.theavarice.enchant.cost.none"));
            return lines;
        }
        for (int i = 0; i < preview.changed().size(); i++) {
            Row row = preview.changed().get(i);
            int target = preview.changes().get(i).target();
            ChatFormatting color = row.enchantment.isCurse() ? ChatFormatting.RED
                    : target > row.current ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
            String arrow = target > row.current ? "▲ " : "▼ ";
            Component change = target == 0
                    ? Component.translatable("gui.theavarice.enchant.preview.remove")
                    : Component.literal(Integer.toString(target));
            lines.add(Component.literal(arrow).append(Component.translatable(row.enchantment.getDescriptionId()))
                    .append(" " + row.current + " → ").append(change).withStyle(color));
        }
        lines.add(Component.empty());
        if (preview.creative()) {
            lines.add(Component.translatable("gui.theavarice.enchant.cost.creative").withStyle(ChatFormatting.GREEN));
            return lines;
        }
        CostMath.Quote q = preview.quote();
        if (q.upgradeCost().signum() > 0) lines.add(amountLine("gui.theavarice.enchant.preview.upgrade", q.upgradeCost(), ChatFormatting.WHITE));
        if (q.curseCost().signum() > 0) lines.add(amountLine("gui.theavarice.enchant.preview.curse", q.curseCost(), ChatFormatting.RED));
        if (q.refund().signum() > 0) {
            lines.add(amountLine("gui.theavarice.enchant.preview.refund", q.refund(), ChatFormatting.GREEN));
            EnchantRules rules = rules();
            if (rules != null) {
                lines.add(Component.translatable("gui.theavarice.enchant.preview.refund_rule",
                        percent(rules.params().refundRatio()), percent(rules.params().naturalRefundRatio()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        BigInteger net = q.net();
        if (net.signum() >= 0) {
            lines.add(amountLine("gui.theavarice.enchant.preview.total_pay", net, preview.affordable() ? ChatFormatting.WHITE : ChatFormatting.RED));
        } else {
            lines.add(amountLine("gui.theavarice.enchant.preview.total_refund", net.negate(), ChatFormatting.GREEN));
        }
        if (!preview.affordable()) lines.add(Component.translatable("gui.theavarice.enchant.insufficient").withStyle(ChatFormatting.RED));
        return lines;
    }

    private List<Component> costPanelLines(Preview preview) {
        List<Component> lines = new ArrayList<>();
        if (preview.hasChanges() && !preview.creative()) {
            BigInteger net = preview.quote().net();
            lines.add(amountLine(net.signum() >= 0 ? "gui.theavarice.enchant.preview.total_pay"
                    : "gui.theavarice.enchant.preview.total_refund", net.abs(), ChatFormatting.WHITE));
        }
        lines.add(amountLine("gui.theavarice.enchant.balance", preview.budget(), ChatFormatting.GRAY));
        return lines;
    }

    private Component amountLine(String key, BigInteger amount, ChatFormatting color) {
        return Component.translatable(key).append(" ").append(amountText(amount)).withStyle(color);
    }

    private Component amountText(BigInteger amount) {
        List<WalletEntry> entries = wallet();
        BigInteger[] parts = WalletMath.breakdown(walletValues(), amount);
        MutableComponent out = Component.empty();
        boolean first = true;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].signum() == 0) continue;
            if (!first) out.append(", ");
            first = false;
            ItemStack icon = iconOf(entries.get(i).id());
            out.append(format.format(parts[i]) + " ")
                    .append(icon.isEmpty() ? Component.literal(entries.get(i).id().toString()) : icon.getHoverName());
        }
        if (first) out.append("0");
        return out;
    }

    private static String percent(double ratio) {
        return Math.round(ratio * 1000.0) / 10.0 + "%";
    }

    private static Component gray(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    private EditBox focusedBox() {
        if (getFocused() instanceof EditBox box) {
            for (Row row : rows) if (row.box == box) return box;
        }
        return null;
    }

    private Row rowOf(EditBox box) {
        for (Row row : rows) if (row.box == box) return row;
        return null;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void focusRow(int index) {
        if (rows.isEmpty()) return;
        index = Math.floorMod(index, rows.size());
        if (index < scroll) scroll = index;
        else if (index >= scroll + EnchantMenu.VISIBLE_ROWS) scroll = index - EnchantMenu.VISIBLE_ROWS + 1;
        layoutBoxes();
        setFocused(rows.get(index).box);
    }

    private void adjust(Row row, int delta) {
        int base = parse(row.text);
        if (base < 0 || !row.selected) base = row.current;
        int value = Mth.clamp(base + delta, 0, maxTarget(row));
        row.box.setValue(Integer.toString(value));
        row.selected = true;
    }

    private static int step() {
        return hasControlDown() ? 100 : hasShiftDown() ? 10 : 1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        EditBox focused = focusedBox();
        if (focused != null) {
            Row row = rowOf(focused);
            switch (keyCode) {
                case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> setFocused(null);
                case GLFW.GLFW_KEY_TAB -> focusRow(rows.indexOf(row) + (hasShiftDown() ? -1 : 1));
                case GLFW.GLFW_KEY_UP -> adjust(row, step());
                case GLFW.GLFW_KEY_DOWN -> adjust(row, -step());
                default -> focused.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (WalletKeys.OPEN_WALLET.getKey().getValue() == keyCode
                && WalletKeys.OPEN_WALLET.getKeyModifier().isActive(null)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        EditBox focused = focusedBox();
        if (focused != null) {
            focused.charTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Row row : rows) {
            if (row.box.isVisible() && row.box.isMouseOver(mouseX, mouseY)) {
                setFocused(row.box);
                row.box.mouseClicked(mouseX, mouseY, button);
                if (button == 1) row.box.setValue("");
                return true;
            }
        }
        if (focusedBox() != null) setFocused(null);

        if (button == 0) {
            if (inside(mouseX, mouseY, BACK_X, BACK_Y, BACK_W, TOP_BTN_H)) {
                click();
                CursorKeeper.remember();
                WalletNetwork.CHANNEL.sendToServer(new SwitchPagePacket(SwitchPagePacket.Page.WALLET));
                return true;
            }
            int by = EnchantMenu.BUTTONS_Y;
            if (inside(mouseX, mouseY, RESET_X, by, RESET_W, BTN_H)) {
                click();
                for (Row row : rows) {
                    row.selected = false;
                    row.box.setValue("");
                }
                return true;
            }
            if (inside(mouseX, mouseY, APPLY_X, by, APPLY_W, BTN_H)) {
                apply();
                return true;
            }
        }

        int index = rowAt(mouseX, mouseY);
        if (index >= 0 && button == 0) {
            Row row = rows.get(index);
            int top = rowTop(index);
            if (inside(mouseX, mouseY, MAX_X, top + 2, MAX_W, MAX_H)) {
                if (upgradable(row)) {
                    click();
                    fillMax(row);
                }
                return true;
            }
            click();
            row.selected = !row.selected;
            if (row.selected && row.text.isEmpty()) row.box.setValue(Integer.toString(row.current));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void fillMax(Row row) {
        EnchantRules rules = rules();
        if (rules == null) return;
        List<CostMath.Change> others = new ArrayList<>();
        for (Row other : rows) {
            if (other == row) continue;
            int target = targetOf(other);
            if (target != other.current) others.add(changeOf(other, target));
        }
        int max = CostMath.maxAffordable(rules.params(), others, changeOf(row, row.current), walletTotal(), creative());
        row.selected = true;
        row.box.setValue(Integer.toString(max));
    }

    private void apply() {
        Preview preview = preview();
        if (!canApply(preview)) return;
        click();
        List<EnchantService.Request> requests = new ArrayList<>();
        for (int i = 0; i < preview.changed().size(); i++) {
            requests.add(new EnchantService.Request(preview.changed().get(i).id, preview.changes().get(i).target()));
        }
        BigInteger expected = preview.creative() ? BigInteger.ZERO : preview.quote().net();
        WalletNetwork.CHANNEL.sendToServer(new EnchantApplyPacket(requests, expected));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (Row row : rows) {
            if (row.box.isVisible() && row.box.isMouseOver(mouseX, mouseY)) {
                adjust(row, (int) Math.signum(delta) * step());
                return true;
            }
        }
        if (maxScroll() > 0 && rowAt(mouseX, mouseY) >= 0) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
            layoutBoxes();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}

package com.eisiadev.theavarice.client;

import com.eisiadev.theavarice.currency.CurrencyTiers;
import com.eisiadev.theavarice.network.SwitchPagePacket;
import com.eisiadev.theavarice.network.WalletActionPacket;
import com.eisiadev.theavarice.network.WalletNetwork;
import com.eisiadev.theavarice.wallet.ClientWalletState;
import com.eisiadev.theavarice.wallet.WalletAction;
import com.eisiadev.theavarice.wallet.WalletEntry;
import com.eisiadev.theavarice.wallet.WalletMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
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

    private final NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
    private int scroll;

    public WalletScreen(WalletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WalletMenu.WIDTH;
        this.imageHeight = WalletMenu.HEIGHT;
        this.titleLabelY = 6;
        this.inventoryLabelY = WalletMenu.INV_Y - 10;
    }

    private List<WalletEntry> entries() {
        return ClientWalletState.get();
    }

    private int maxScroll() {
        return Math.max(0, entries().size() - WalletMenu.VISIBLE_ROWS);
    }

    private int rowAt(double mouseX, double mouseY) {
        double relX = mouseX - leftPos;
        double relY = mouseY - topPos - WalletMenu.ROWS_Y;
        if (relX < ROW_X || relX >= ROW_X + ROW_W) return -1;
        if (relY < 0 || relY >= WalletMenu.VISIBLE_ROWS * WalletMenu.ROW_H) return -1;
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

    private static ItemStack stackOf(WalletEntry entry) {
        Item item = ForgeRegistries.ITEMS.getValue(entry.id());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        scroll = Mth.clamp(scroll, 0, maxScroll());
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderWalletTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 2, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFFC6C6C6);

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

        List<WalletEntry> list = entries();
        int hoveredRow = rowAt(mouseX, mouseY);
        int hoveredZone = zoneAt(mouseX);

        for (int i = 0; i < WalletMenu.VISIBLE_ROWS; i++) {
            int display = scroll + i;
            if (display >= list.size()) break;
            int index = list.size() - 1 - display;

            WalletEntry entry = list.get(index);
            int rowY = y + WalletMenu.ROWS_Y + i * WalletMenu.ROW_H;
            boolean rowHovered = index == hoveredRow;

            graphics.fill(x + ROW_X, rowY, x + ROW_X + ROW_W, rowY + WalletMenu.ROW_H - 2,
                    rowHovered && hoveredZone == ZONE_BODY ? 0xFFA8A8A8 : 0xFF8B8B8B);

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
            int trackTop = y + WalletMenu.ROWS_Y;
            int trackHeight = WalletMenu.VISIBLE_ROWS * WalletMenu.ROW_H - 2;
            int thumbHeight = Math.max(8, trackHeight * WalletMenu.VISIBLE_ROWS / list.size());
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll();
            graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackHeight, 0xFF373737);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFFFFFFF);
        }
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && onEnchantButton(mouseX, mouseY)) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CursorKeeper.remember();
            WalletNetwork.CHANNEL.sendToServer(new SwitchPagePacket(SwitchPagePacket.Page.ENCHANT));
            return true;
        }
        int index = rowAt(mouseX, mouseY);
        if (index >= 0 && (button == 0 || button == 1)) {
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
                WalletNetwork.CHANNEL.sendToServer(new WalletActionPacket(index, action));
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
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

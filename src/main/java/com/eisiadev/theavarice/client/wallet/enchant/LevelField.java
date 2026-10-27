package com.eisiadev.theavarice.client.wallet.enchant;

import com.eisiadev.theavarice.client.wallet.Gfx;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.value.StringValue;
import brachy.modularui.widgets.textfield.TextFieldWidget;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Target level input for one visible enchant row. Wheel / up-down arrows: ±1 (Shift ±10, Ctrl ±100),
 * right click: clear.
 */
public class LevelField extends TextFieldWidget {

    private static final Pattern DIGITS = Pattern.compile("[0-9]*");

    private static final IDrawable BACKGROUND = (context, x, y, w, h, theme) -> {
        GuiGraphics g = context.getGraphics();
        Gfx.rect(g, x, y, w, h, Gfx.FRAME);
        Gfx.rect(g, x + 1, y + 1, w - 2, h - 2, Gfx.LED_BG);
        Gfx.rect(g, x + 1, y + h - 2, w - 2, 1, 0xFF143014);
    };

    private final EnchantView enchant;
    private final int slot;

    public LevelField(EnchantView enchant, int slot) {
        this.enchant = enchant;
        this.slot = slot;
        setPattern(DIGITS);
        setMaxLength(5);
        autoUpdateOnChange(true);
        setTextColor(Gfx.LED_ON);
        hintText(Component.literal("-"));
        hintColor(Gfx.LED_GHOST);
        disableThemeBackground(true);
        background(BACKGROUND);
        value(new StringValue.Dynamic(() -> {
            EnchantView.Row row = enchant.row(slot);
            return row == null ? "" : row.text;
        }, text -> {
            EnchantView.Row row = enchant.row(slot);
            if (row != null) enchant.setText(row, text);
        }));
        setEnabledIf(w -> enchant.row(slot) != null);
        tooltipDynamic(t -> {
            t.clearText();
            EnchantView.Row row = enchant.row(slot);
            if (row == null) return;
            t.addLine(Component.translatable("gui.theavarice.enchant.box.tooltip", enchant.maxTarget(row)));
            t.addLine(Component.translatable("gui.theavarice.enchant.box.wheel").withStyle(ChatFormatting.GRAY));
        });
    }

    private void adjust(int delta) {
        EnchantView.Row row = enchant.row(slot);
        if (row == null) return;
        enchant.adjust(row, delta);
        setText(row.text);
    }

    @Override
    public boolean onMouseScrolled(double delta) {
        adjust((delta > 0 ? 1 : -1) * EnchantView.step());
        return true;
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button == 1) {
            EnchantView.Row row = enchant.row(slot);
            if (row != null) enchant.setText(row, "");
            setText("");
            return Result.SUCCESS;
        }
        return super.onMousePressed(button);
    }

    @Override
    public @NotNull Result onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (isFocused() && keyCode == InputConstants.KEY_UP) {
            adjust(EnchantView.step());
            return Result.SUCCESS;
        }
        if (isFocused() && keyCode == InputConstants.KEY_DOWN) {
            adjust(-EnchantView.step());
            return Result.SUCCESS;
        }
        return super.onKeyPressed(keyCode, scanCode, modifiers);
    }
}

package com.eisiadev.theavarice.client.wallet;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.client.WalletKeys;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;

/** The wallet screen. Pressing the wallet key again closes it, like the old screen. */
public class WalletScreen extends ModularScreen {

    public WalletScreen(ModularPanel<?> panel) {
        super(TheAvarice.MOD_ID, panel);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (WalletKeys.OPEN_WALLET.getKey().getValue() == keyCode
                && WalletKeys.OPEN_WALLET.getKeyModifier().isActive(null)) {
            close();
            return true;
        }
        return false;
    }
}

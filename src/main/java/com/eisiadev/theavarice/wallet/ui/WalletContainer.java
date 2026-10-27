package com.eisiadev.theavarice.wallet.ui;

import brachy.modularui.screen.ModularContainerMenu;

/** The wallet's ModularUI container. Only exists so the server can tell "this player has the wallet open". */
public class WalletContainer extends ModularContainerMenu {

    public WalletContainer(int containerId) {
        super(containerId);
    }
}

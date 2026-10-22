package com.eisiadev.theavarice.wallet;

import java.util.List;

public final class ClientWalletState {

    private static volatile List<WalletEntry> entries = List.of();

    private ClientWalletState() {}

    public static List<WalletEntry> get() {
        return entries;
    }

    public static void set(List<WalletEntry> value) {
        entries = List.copyOf(value);
    }
}

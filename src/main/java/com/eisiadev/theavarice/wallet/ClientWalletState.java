package com.eisiadev.theavarice.wallet;

import com.eisiadev.theavarice.currency.CurrencyGroup;

import java.util.List;

public final class ClientWalletState {

    private static volatile List<WalletGroupView> groups = List.of();

    private ClientWalletState() {}

    /** Entries of the main (configured) currency group. */
    public static List<WalletEntry> get() {
        for (WalletGroupView group : groups) {
            if (group.key().equals(CurrencyGroup.MAIN_KEY)) return group.entries();
        }
        return List.of();
    }

    /** Every currency group the server handles, main group first. */
    public static List<WalletGroupView> groups() {
        return groups;
    }

    public static void set(List<WalletGroupView> value) {
        groups = List.copyOf(value);
    }
}

package com.eisiadev.theavarice.enchant;

public final class ClientEnchantState {

    public record Status(boolean success, String key, long timeMillis) {}

    private static volatile EnchantRules rules;
    private static volatile Status status;
    private static volatile int statusSerial;

    private ClientEnchantState() {}

    public static EnchantRules rules() {
        return rules;
    }

    public static void setRules(EnchantRules value) {
        rules = value;
    }

    public static Status status() {
        return status;
    }

    public static int statusSerial() {
        return statusSerial;
    }

    public static void setStatus(boolean success, String key) {
        status = new Status(success, key, System.currentTimeMillis());
        statusSerial++;
    }
}

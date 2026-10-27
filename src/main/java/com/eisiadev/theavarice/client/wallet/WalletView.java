package com.eisiadev.theavarice.client.wallet;

import com.eisiadev.theavarice.currency.CurrencyGroup;
import com.eisiadev.theavarice.currency.CurrencyTiers;
import com.eisiadev.theavarice.currency.WalletMath;
import com.eisiadev.theavarice.wallet.WalletAction;
import com.eisiadev.theavarice.wallet.WalletEntry;
import com.eisiadev.theavarice.wallet.WalletGroupView;
import com.eisiadev.theavarice.wallet.ui.WalletUI;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Client-side model of one open wallet screen: the currency groups, the live balances (ModularUI sync values),
 * which group is shown, and the short-lived events (receipt lines, coins in the chute, flashes) the widgets draw.
 */
public final class WalletView {

    public enum Mode {
        ONE(WalletAction.WITHDRAW_ONE, "gui.theavarice.wallet.mode.one"),
        STACK(WalletAction.WITHDRAW_STACK, "gui.theavarice.wallet.mode.stack"),
        ALL(WalletAction.WITHDRAW_ALL, "gui.theavarice.wallet.mode.all");

        public final WalletAction action;
        public final String key;

        Mode(WalletAction action, String key) {
            this.action = action;
            this.key = key;
        }
    }

    public enum Status { READY, DISPENSE, EXCHANGE, INCOMING, DENIED }

    public enum Page { WALLET, ENCHANT }

    public enum LineKind { OUT, IN, EXCHANGE }

    public record ReceiptLine(int serial, LineKind kind, String text, long time) {}

    // Remembered for the session, like the old screen's tabs.
    private static CurrencyGroup.Category lastCategory = CurrencyGroup.Category.MAIN;
    private static int lastRegion = 0;
    private static Mode lastMode = Mode.STACK;
    private static int receiptSerial = 0;
    private static Page lastPage = Page.WALLET;

    private static final int MAX_RECEIPT = 24;
    private static final long PENDING_TIMEOUT = 3000;
    private static final long STATUS_TIME = 900;

    private final PanelSyncManager sync;
    private final BooleanSyncValue auto;
    private final Component title;
    private final List<WalletGroupView> groups;
    private final LongSyncValue[][] counts;
    private final long[][] previous;
    private final long[][] flash;
    private final ItemStack[][] stacks;
    private final int[][] colors;
    private final List<CurrencyGroup.Category> categories = new ArrayList<>();
    private final NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);

    private final Deque<ReceiptLine> receipt = new ArrayDeque<>();

    private int pendingGroup = -1;
    private int pendingIndex = -1;
    private WalletAction pendingAction;
    private long pendingTime;

    private Status status = Status.READY;
    private long statusTime;

    // Converter hover: tubes on both sides of the hovered exchange button light up.
    private int linkGroup = -1;
    private int linkBoundary = -1;
    private long linkTime;

    // Rolling total on the LED.
    private int totalGroup = -1;
    private BigInteger totalFrom = BigInteger.ZERO;
    private BigInteger totalTo = BigInteger.ZERO;
    private long totalTime;

    public int regionScroll;

    public WalletView(PanelSyncManager sync, Component title, List<WalletGroupView> groups, LongSyncValue[][] counts,
                      BooleanSyncValue auto) {
        this.sync = sync;
        this.auto = auto;
        this.title = title;
        this.groups = groups;
        this.counts = counts;
        this.previous = new long[groups.size()][];
        this.flash = new long[groups.size()][];
        this.stacks = new ItemStack[groups.size()][];
        this.colors = new int[groups.size()][];
        for (int g = 0; g < groups.size(); g++) {
            List<WalletEntry> entries = groups.get(g).entries();
            previous[g] = new long[entries.size()];
            flash[g] = new long[entries.size()];
            stacks[g] = new ItemStack[entries.size()];
            colors[g] = new int[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                previous[g][i] = counts[g][i].getLongValue();
                stacks[g][i] = stackOf(entries.get(i).id());
                colors[g][i] = TierColors.of(entries.get(i).id(), i);
            }
        }
        for (CurrencyGroup.Category category : CurrencyGroup.Category.values()) {
            for (WalletGroupView group : groups) {
                if (group.category() == category) {
                    categories.add(category);
                    break;
                }
            }
        }
        ensureRegionVisible();
    }

    // ---------- Structure ----------

    public Component title() {
        return title;
    }

    public List<WalletGroupView> groups() {
        return groups;
    }

    public WalletGroupView group(int g) {
        return groups.get(g);
    }

    public int tierCount(int g) {
        return groups.get(g).entries().size();
    }

    public List<CurrencyGroup.Category> categories() {
        return categories;
    }

    public CurrencyGroup.Category category() {
        if (categories.isEmpty() || categories.contains(lastCategory)) return lastCategory;
        return categories.get(0);
    }

    public List<Integer> groupsOf(CurrencyGroup.Category category) {
        List<Integer> out = new ArrayList<>();
        for (int g = 0; g < groups.size(); g++) {
            if (groups.get(g).category() == category) out.add(g);
        }
        return out;
    }

    public List<Integer> regions() {
        return groupsOf(CurrencyGroup.Category.TOKENS);
    }

    public int region() {
        return Mth.clamp(lastRegion, 0, Math.max(0, regions().size() - 1));
    }

    public boolean showsRegions() {
        return category() == CurrencyGroup.Category.TOKENS && !regions().isEmpty();
    }

    /** Index of the group on screen, or -1 when the server has no currencies at all. */
    public int current() {
        List<Integer> inCategory = groupsOf(category());
        if (inCategory.isEmpty()) return -1;
        return category() == CurrencyGroup.Category.TOKENS ? inCategory.get(region()) : inCategory.get(0);
    }

    public void selectCategory(CurrencyGroup.Category category) {
        if (category == category()) return;
        lastCategory = category;
        ensureRegionVisible();
        click(1.0F);
    }

    public void selectRegion(int region) {
        if (region == region()) return;
        lastRegion = region;
        click(1.1F);
    }

    public static final int REGION_SLOTS = 8;

    public int maxRegionScroll() {
        return Math.max(0, regions().size() - REGION_SLOTS);
    }

    public void scrollRegions(int delta) {
        int before = regionScroll;
        regionScroll = Mth.clamp(regionScroll + delta, 0, maxRegionScroll());
        if (before != regionScroll) click(1.3F);
    }

    private void ensureRegionVisible() {
        int region = region();
        if (region < regionScroll) regionScroll = region;
        if (region >= regionScroll + REGION_SLOTS) regionScroll = region - REGION_SLOTS + 1;
        regionScroll = Mth.clamp(regionScroll, 0, maxRegionScroll());
    }

    public Mode mode() {
        return lastMode;
    }

    public void setMode(Mode mode) {
        if (mode == lastMode) return;
        lastMode = mode;
        click(1.4F);
    }

    // ---------- Balances ----------

    public long count(int g, int i) {
        return counts[g][i].getLongValue();
    }

    public ItemStack stack(int g, int i) {
        return stacks[g][i];
    }

    /** The group's own icon (main: top tier, tokens: the region's base token). */
    public ItemStack icon(int g) {
        return stackOf(groups.get(g).icon());
    }

    public int color(int g, int i) {
        return colors[g][i];
    }

    public String name(int g, int i) {
        ItemStack stack = stacks[g][i];
        return stack.isEmpty() ? groups.get(g).entries().get(i).id().getPath() : stack.getHoverName().getString();
    }

    public long value(int g, int i) {
        return groups.get(g).entries().get(i).value();
    }

    /** How many of tier {@code i} make one of tier {@code i - 1}, or 0 if they don't divide. */
    public long ratio(int g, int i) {
        if (i <= 0 || i >= tierCount(g)) return 0;
        return CurrencyTiers.ratio(value(g, i - 1), value(g, i));
    }

    public BigInteger total(int g) {
        int n = tierCount(g);
        long[] c = new long[n];
        long[] v = new long[n];
        for (int i = 0; i < n; i++) {
            c[i] = count(g, i);
            v[i] = value(g, i);
        }
        return WalletMath.total(c, v);
    }

    /** Total shown on the LED, rolling towards the real value after a change. */
    public BigInteger displayedTotal(int g, long now) {
        BigInteger real = total(g);
        if (g != totalGroup) {
            totalGroup = g;
            totalFrom = real;
            totalTo = real;
            totalTime = 0;
            return real;
        }
        if (!real.equals(totalTo)) {
            totalFrom = displayed(now);
            totalTo = real;
            totalTime = now;
        }
        return displayed(now);
    }

    private BigInteger displayed(long now) {
        if (totalTime == 0) return totalTo;
        float t = (now - totalTime) / 550f;
        if (t >= 1) return totalTo;
        long k = (long) (Gfx.easeOutCubic(t) * 10_000);
        return totalFrom.add(totalTo.subtract(totalFrom).multiply(BigInteger.valueOf(k)).divide(BigInteger.valueOf(10_000)));
    }

    public boolean canConvertUp(int g, int i) {
        long ratio = ratio(g, i);
        return ratio > 0 && count(g, i) >= ratio;
    }

    public boolean canConvertDown(int g, int i) {
        return ratio(g, i + 1) > 0 && count(g, i) >= 1;
    }

    // ---------- Actions ----------

    public void withdraw(int g, int i, WalletAction action) {
        if (count(g, i) <= 0) {
            deny();
            return;
        }
        send(g, i, action);
        click(1.6F);
    }

    /** {@code boundary} is the index of the lower tier: exchange between tiers {@code boundary - 1} and {@code boundary}. */
    public void convertUp(int g, int boundary, boolean all) {
        if (!canConvertUp(g, boundary)) {
            deny();
            return;
        }
        send(g, boundary, all ? WalletAction.CONVERT_UP_ALL : WalletAction.CONVERT_UP);
        sound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.4F, 0.35F);
    }

    public void convertDown(int g, int boundary, boolean all) {
        if (!canConvertDown(g, boundary - 1)) {
            deny();
            return;
        }
        send(g, boundary - 1, all ? WalletAction.CONVERT_DOWN_ALL : WalletAction.CONVERT_DOWN);
        sound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9F, 0.35F);
    }

    private void send(int g, int index, WalletAction action) {
        pendingGroup = g;
        pendingIndex = index;
        pendingAction = action;
        pendingTime = Util.getMillis();
        WalletUI.sendAction(sync, groups.get(g).key(), index, action);
    }

    private void deny() {
        setStatus(Status.DENIED);
        sound(net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 1.6F, 0.4F);
    }

    /** Asks the server to move everything in the coin-out tray into the inventory now. */
    public void collect() {
        sync.callSyncedAction(WalletUI.ACTION_COLLECT);
    }

    public boolean autoDeposit() {
        return auto.getBoolValue();
    }

    public void toggleAutoDeposit() {
        sync.callSyncedAction(WalletUI.ACTION_TOGGLE_AUTO);
        click(autoDeposit() ? 0.8F : 1.3F);
    }

    public PanelSyncManager sync() {
        return sync;
    }

    // ---------- Pages ----------

    public Page page() {
        return lastPage;
    }

    public void togglePage() {
        lastPage = lastPage == Page.WALLET ? Page.ENCHANT : Page.WALLET;
        click(1.0F);
    }

    /** Index of the main (configured) currency group, which pays for enchantments; -1 if there is none. */
    public int mainGroup() {
        for (int g = 0; g < groups.size(); g++) {
            if (groups.get(g).key().equals(CurrencyGroup.MAIN_KEY)) return g;
        }
        return -1;
    }

    // ---------- Hover links ----------

    public void hoverLink(int g, int boundary) {
        linkGroup = g;
        linkBoundary = boundary;
        linkTime = Util.getMillis();
    }

    /** True if tier {@code i} sits next to the exchange button the mouse is on. */
    public boolean linked(int g, int i) {
        if (g != linkGroup || Util.getMillis() - linkTime > 60) return false;
        return i == linkBoundary || i == linkBoundary - 1;
    }

    public long flashTime(int g, int i) {
        return flash[g][i];
    }

    // ---------- Status, receipt, chute ----------

    public Status status(long now) {
        if (status != Status.READY && now - statusTime > STATUS_TIME) status = Status.READY;
        return status;
    }

    private void setStatus(Status value) {
        status = value;
        statusTime = Util.getMillis();
    }

    public Iterable<ReceiptLine> receipt() {
        return receipt;
    }

    public int receiptSize() {
        return receipt.size();
    }

    /** Compares balances with last tick and turns differences into receipt lines, chute coins and flashes. */
    public void tick() {
        long now = Util.getMillis();
        if (pendingAction != null && now - pendingTime > PENDING_TIMEOUT) pendingAction = null;

        for (int g = 0; g < groups.size(); g++) {
            boolean changed = false;
            long[] deltas = new long[tierCount(g)];
            for (int i = 0; i < deltas.length; i++) {
                long value = count(g, i);
                deltas[i] = value - previous[g][i];
                if (deltas[i] != 0) {
                    changed = true;
                    flash[g][i] = now;
                    previous[g][i] = value;
                }
            }
            if (changed) describe(g, deltas, now);
        }

    }

    private void describe(int g, long[] deltas, long now) {
        if (pendingAction != null && pendingGroup == g) {
            WalletAction action = pendingAction;
            int index = pendingIndex;
            pendingAction = null;
            switch (action) {
                case WITHDRAW_ONE, WITHDRAW_STACK, WITHDRAW_ALL -> {
                    long out = -deltas[index];
                    if (out > 0) {
                        line(LineKind.OUT, "-" + compact(out) + " " + name(g, index), now);
                        setStatus(Status.DISPENSE);
                        deltas[index] = 0;
                    }
                }
                case CONVERT_UP, CONVERT_UP_ALL -> {
                    if (index > 0 && deltas[index] < 0 && deltas[index - 1] > 0) {
                        line(LineKind.EXCHANGE, compact(-deltas[index]) + " " + name(g, index)
                                + " > " + compact(deltas[index - 1]) + " " + name(g, index - 1), now);
                        setStatus(Status.EXCHANGE);
                        deltas[index] = 0;
                        deltas[index - 1] = 0;
                    }
                }
                case CONVERT_DOWN, CONVERT_DOWN_ALL -> {
                    if (index + 1 < deltas.length && deltas[index] < 0 && deltas[index + 1] > 0) {
                        line(LineKind.EXCHANGE, compact(-deltas[index]) + " " + name(g, index)
                                + " > " + compact(deltas[index + 1]) + " " + name(g, index + 1), now);
                        setStatus(Status.EXCHANGE);
                        deltas[index] = 0;
                        deltas[index + 1] = 0;
                    }
                }
            }
        }
        // Anything left came from outside the screen (picked up coins, another mod...).
        for (int i = 0; i < deltas.length; i++) {
            if (deltas[i] > 0) {
                line(LineKind.IN, "+" + compact(deltas[i]) + " " + name(g, i), now);
                setStatus(Status.INCOMING);
            } else if (deltas[i] < 0) {
                line(LineKind.OUT, "-" + compact(-deltas[i]) + " " + name(g, i), now);
            }
        }
    }

    private void line(LineKind kind, String text, long now) {
        receipt.addFirst(new ReceiptLine(++receiptSerial, kind, text, now));
        while (receipt.size() > MAX_RECEIPT) receipt.removeLast();
    }

    // ---------- Formatting ----------

    public String full(long value) {
        return format.format(value);
    }

    public String full(BigInteger value) {
        return format.format(value);
    }

    /** 999 / 12.3K / 4.56M ... */
    public static String compact(long value) {
        if (value < 10_000) return Long.toString(value);
        String[] units = {"K", "M", "B", "T", "Q"};
        double v = value;
        int u = -1;
        while (v >= 1000 && u < units.length - 1) {
            v /= 1000;
            u++;
        }
        String number = v >= 100 ? String.format(Locale.ROOT, "%.0f", v)
                : v >= 10 ? String.format(Locale.ROOT, "%.1f", v)
                : String.format(Locale.ROOT, "%.2f", v);
        return number + units[u];
    }

    public static String compact(BigInteger value) {
        if (value.bitLength() < 63) return compact(value.longValue());
        return String.format(Locale.ROOT, "%.3e", value.doubleValue());
    }

    // ---------- Sounds ----------

    public static void click(float pitch) {
        sound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25F);
    }

    public static void sound(SoundEvent event, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }

    private static ItemStack stackOf(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}

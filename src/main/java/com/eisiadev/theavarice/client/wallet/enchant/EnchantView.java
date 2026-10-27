package com.eisiadev.theavarice.client.wallet.enchant;

import com.eisiadev.theavarice.client.wallet.WalletView;
import com.eisiadev.theavarice.currency.WalletMath;
import com.eisiadev.theavarice.enchant.ClientEnchantState;
import com.eisiadev.theavarice.enchant.CostMath;
import com.eisiadev.theavarice.enchant.EnchantNbt;
import com.eisiadev.theavarice.enchant.EnchantRules;
import com.eisiadev.theavarice.enchant.EnchantService;
import com.eisiadev.theavarice.wallet.ui.WalletSession;
import com.eisiadev.theavarice.wallet.ui.WalletUI;
import brachy.modularui.api.widget.Interactable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client model of the enchant page (ported from the old EnchantScreen): one row per enchantment on the item in the
 * enchant slot, the target levels typed in, and the cost quote. Pays with the main currency group.
 */
public final class EnchantView {

    public static final int VISIBLE_ROWS = 5;
    private static final long STATUS_MILLIS = 4000L;

    public static final class Row {
        public final ResourceLocation id;
        public final Enchantment enchantment;
        public final int current;
        public final int purchased;
        public boolean selected;
        public String text = "";

        Row(ResourceLocation id, Enchantment enchantment, int current, int purchased) {
            this.id = id;
            this.enchantment = enchantment;
            this.current = current;
            this.purchased = purchased;
        }
    }

    public record Preview(List<Row> changed, List<CostMath.Change> changes, CostMath.Quote quote,
                          BigInteger budget, boolean creative) {
        public boolean hasChanges() {
            return !changes.isEmpty();
        }

        public boolean affordable() {
            return creative || quote.net().compareTo(budget) <= 0;
        }
    }

    private final WalletView wallet;
    private final WalletSession session;
    private final List<Row> rows = new ArrayList<>();
    private final int statusSerialAtOpen = ClientEnchantState.statusSerial();
    private ItemStack snapshot = ItemStack.EMPTY;
    public int scroll;

    public EnchantView(WalletView wallet, WalletSession session) {
        this.wallet = wallet;
        this.session = session;
    }

    // ---------- Rows ----------

    public ItemStack target() {
        return session.target.getStackInSlot(0);
    }

    public List<Row> rows() {
        return rows;
    }

    /** Row shown in visible slot {@code slot}, or null. */
    public Row row(int slot) {
        int index = scroll + slot;
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }

    public int maxScroll() {
        return Math.max(0, rows.size() - VISIBLE_ROWS);
    }

    public void scrollBy(int delta) {
        scroll = Mth.clamp(scroll + delta, 0, maxScroll());
    }

    /** Called every client tick: rebuilds the rows when the item in the enchant slot changes. */
    public void tick() {
        ItemStack target = target();
        if (ItemStack.matches(snapshot, target)) return;
        snapshot = target.copy();
        rows.clear();
        scroll = 0;
        if (snapshot.isEmpty()) return;
        Map<ResourceLocation, Integer> purchased = EnchantNbt.readPurchased(snapshot);
        EnchantNbt.read(snapshot).forEach((id, level) -> {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
            if (enchantment == null) return;
            rows.add(new Row(id, enchantment, level, Math.min(level, purchased.getOrDefault(id, 0))));
        });
    }

    public void setText(Row row, String text) {
        if (!text.isEmpty()) {
            int value = parse(text);
            int max = maxTarget(row);
            if (value > max) text = Integer.toString(max);
            row.selected = true;
        }
        row.text = text;
    }

    public void toggle(Row row) {
        row.selected = !row.selected;
        if (row.selected && row.text.isEmpty()) row.text = Integer.toString(row.current);
        WalletView.click(1.0F);
    }

    public void adjust(Row row, int delta) {
        int base = parse(row.text);
        if (base < 0 || !row.selected) base = row.current;
        int value = Mth.clamp(base + delta, 0, maxTarget(row));
        row.text = Integer.toString(value);
        row.selected = true;
    }

    public static int step() {
        return Interactable.hasControlDown() ? 100 : Interactable.hasShiftDown() ? 10 : 1;
    }

    public boolean anySelected() {
        for (Row row : rows) if (row.selected || !row.text.isEmpty()) return true;
        return false;
    }

    public void reset() {
        for (Row row : rows) {
            row.selected = false;
            row.text = "";
        }
    }

    // ---------- Rules ----------

    public EnchantRules rules() {
        return ClientEnchantState.rules();
    }

    public boolean creative() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().player.getAbilities().instabuild;
    }

    public boolean upgradable(Row row) {
        EnchantRules rules = rules();
        return rules != null && rules.canUpgrade(row.id, row.enchantment, row.current);
    }

    public int maxTarget(Row row) {
        EnchantRules rules = rules();
        if (rules == null) return row.current;
        return upgradable(row) ? Math.max(row.current, rules.capFor(row.id)) : row.current;
    }

    public int naturalMax(Row row) {
        EnchantRules rules = rules();
        return rules == null ? row.enchantment.getMaxLevel() : rules.naturalMaxFor(row.id, row.enchantment);
    }

    public int targetOf(Row row) {
        if (!row.selected) return row.current;
        int value = parse(row.text);
        if (value < 0) return row.current;
        return Math.min(value, maxTarget(row));
    }

    private static int parse(String text) {
        if (text == null || text.isEmpty()) return -1;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    // ---------- Money ----------

    private int main() {
        return wallet.mainGroup();
    }

    public long[] walletValues() {
        int g = main();
        if (g < 0) return new long[0];
        long[] values = new long[wallet.tierCount(g)];
        for (int i = 0; i < values.length; i++) values[i] = wallet.value(g, i);
        return values;
    }

    public BigInteger walletTotal() {
        int g = main();
        return g < 0 ? BigInteger.ZERO : wallet.total(g);
    }

    /** Splits an amount into main-currency coins, most valuable first: {tier index, count} pairs. */
    public List<long[]> breakdown(BigInteger amount) {
        List<long[]> out = new ArrayList<>();
        int g = main();
        if (g < 0) return out;
        BigInteger[] parts = WalletMath.breakdown(walletValues(), amount);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].signum() != 0) out.add(new long[]{i, parts[i].min(BigInteger.valueOf(Long.MAX_VALUE)).longValue()});
        }
        return out;
    }

    public ItemStack coin(int tier) {
        int g = main();
        return g < 0 ? ItemStack.EMPTY : wallet.stack(g, tier);
    }

    // ---------- Quote ----------

    private CostMath.Change changeOf(Row row, int target) {
        return new CostMath.Change(rules().curve(row.id, row.enchantment), row.current, row.purchased, target);
    }

    public Preview preview() {
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

    public boolean canApply() {
        Preview preview = preview();
        return rules() != null && preview.hasChanges() && preview.affordable();
    }

    public void fillMax(Row row) {
        EnchantRules rules = rules();
        if (rules == null || !upgradable(row)) return;
        List<CostMath.Change> others = new ArrayList<>();
        for (Row other : rows) {
            if (other == row) continue;
            int target = targetOf(other);
            if (target != other.current) others.add(changeOf(other, target));
        }
        int max = CostMath.maxAffordable(rules.params(), others, changeOf(row, row.current), walletTotal(), creative());
        row.selected = true;
        row.text = Integer.toString(max);
        WalletView.click(1.2F);
    }

    public void apply() {
        Preview preview = preview();
        if (!(rules() != null && preview.hasChanges() && preview.affordable())) return;
        List<EnchantService.Request> requests = new ArrayList<>();
        for (int i = 0; i < preview.changed().size(); i++) {
            requests.add(new EnchantService.Request(preview.changed().get(i).id, preview.changes().get(i).target()));
        }
        BigInteger expected = preview.creative() ? BigInteger.ZERO : preview.quote().net();
        WalletUI.sendEnchant(wallet.sync(), requests, expected);
    }

    /** Server status of the last apply, if it is recent. */
    public ClientEnchantState.Status status() {
        ClientEnchantState.Status status = ClientEnchantState.status();
        boolean show = status != null && ClientEnchantState.statusSerial() != statusSerialAtOpen
                && System.currentTimeMillis() - status.timeMillis() < STATUS_MILLIS;
        return show ? status : null;
    }

    // ---------- Tooltip text ----------

    public List<Component> rowLines(Row row) {
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

    public List<Component> previewLines() {
        Preview preview = preview();
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

    public List<Component> costLines() {
        Preview preview = preview();
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

    public Component amountText(BigInteger amount) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (long[] part : breakdown(amount)) {
            if (!first) out.append(", ");
            first = false;
            ItemStack icon = coin((int) part[0]);
            out.append(wallet.full(part[1]) + " ").append(icon.isEmpty() ? Component.literal("?") : icon.getHoverName());
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
}

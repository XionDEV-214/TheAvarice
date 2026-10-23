package com.eisiadev.theavarice.enchant;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CostMath {

    public static final int ABSOLUTE_MAX_LEVEL = Short.MAX_VALUE;

    private CostMath() {}

    public record Curve(double weight, int vanillaMax, int cap, boolean curse) {}

    public record Change(Curve curve, int current, int purchased, int target) {}

    public record Result(int newLevel, int newPurchased) {}

    public record Quote(BigInteger upgradeCost, BigInteger curseCost, BigInteger refund, List<Result> results) {
        public BigInteger net() {
            return upgradeCost.add(curseCost).subtract(refund);
        }

        public boolean isEmpty() {
            return results.isEmpty();
        }
    }

    public static double rawStep(CostParams p, Curve c, int level) {
        double lv = level;
        if (c.curse()) {
            return p.curseBaseCost() * c.weight() * StrictMath.pow(lv, p.exponent());
        }
        double v = p.baseCost() * c.weight() * StrictMath.pow(lv, p.exponent());
        int start = Math.max(c.vanillaMax(), c.cap() / 2);
        if (c.cap() > start && level > start) {
            double x = (double) (level - start) / (double) (c.cap() - start);
            v *= StrictMath.pow(p.steepness(), x);
        }
        return v;
    }

    public static long step(CostParams p, Curve c, int level) {
        double v = rawStep(p, c, level);
        if (!(v >= 1.0)) return 1L;
        if (v >= 9.0e18) return Long.MAX_VALUE / 4;
        return (long) v;
    }

    private record PrefixKey(CostParams params, Curve curve) {}

    private static final Map<PrefixKey, long[]> PREFIX = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<PrefixKey, long[]> eldest) {
                    return size() > 96;
                }
            });

    private static long[] prefix(CostParams p, Curve c) {
        return PREFIX.computeIfAbsent(new PrefixKey(p, c), key -> {
            long[] arr = new long[ABSOLUTE_MAX_LEVEL + 1];
            boolean overflow = false;
            for (int lv = 1; lv <= ABSOLUTE_MAX_LEVEL; lv++) {
                if (overflow) {
                    arr[lv] = -1;
                    continue;
                }
                long sum = arr[lv - 1] + step(p, c, lv);
                if (sum < arr[lv - 1]) {
                    overflow = true;
                    arr[lv] = -1;
                } else {
                    arr[lv] = sum;
                }
            }
            return arr;
        });
    }

    public static BigInteger rangeCost(CostParams p, Curve c, int from, int to) {
        if (to <= from) return BigInteger.ZERO;
        from = Math.max(0, from);
        if (to <= ABSOLUTE_MAX_LEVEL) {
            long[] pre = prefix(p, c);
            if (pre[to] >= 0) return BigInteger.valueOf(pre[to] - pre[from]);
        }
        BigInteger big = BigInteger.ZERO;
        long acc = 0;
        for (int lv = from + 1; lv <= to; lv++) {
            long s = step(p, c, lv);
            long sum = acc + s;
            if (sum < acc) {
                big = big.add(BigInteger.valueOf(acc));
                acc = s;
            } else {
                acc = sum;
            }
        }
        return big.add(BigInteger.valueOf(acc));
    }

    public static Quote quote(CostParams p, List<Change> changes, boolean creative) {
        BigInteger upgrade = BigInteger.ZERO;
        BigInteger curse = BigInteger.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal refundRatio = BigDecimal.valueOf(Math.max(0.0, p.refundRatio()));
        BigDecimal naturalRatio = BigDecimal.valueOf(Math.max(0.0, p.naturalRefundRatio()));
        List<Result> results = new ArrayList<>(changes.size());

        for (Change ch : changes) {
            int cur = Math.max(0, ch.current());
            int purchased = Math.max(0, Math.min(ch.purchased(), cur));
            int target = Math.max(0, ch.target());

            if (target > cur) {
                if (!creative) {
                    upgrade = upgrade.add(rangeCost(p, ch.curve(), cur, target));
                    purchased += target - cur;
                }
                results.add(new Result(target, purchased));
            } else if (target < cur) {
                int removed = cur - target;
                int removedPurchased = Math.min(purchased, removed);
                if (!creative) {
                    if (ch.curve().curse()) {
                        curse = curse.add(rangeCost(p, ch.curve(), target, cur));
                    } else {
                        int split = cur - removedPurchased;
                        if (removedPurchased > 0) {
                            refund = refund.add(refundRatio.multiply(new BigDecimal(rangeCost(p, ch.curve(), split, cur))));
                        }
                        if (split > target) {
                            refund = refund.add(naturalRatio.multiply(new BigDecimal(rangeCost(p, ch.curve(), target, split))));
                        }
                    }
                }
                results.add(new Result(target, Math.min(purchased - removedPurchased, target)));
            } else {
                results.add(new Result(cur, purchased));
            }
        }

        BigInteger refundInt = refund.setScale(0, RoundingMode.FLOOR).toBigInteger();
        return new Quote(upgrade, curse, refundInt, results);
    }

    public static int maxAffordable(CostParams p, List<Change> others, Change self, BigInteger budget, boolean creative) {
        int lo = self.current();
        int hi = Math.max(lo, self.curve().cap());
        if (creative) return hi;
        if (self.curve().curse() || hi <= lo) return lo;

        BigInteger fixed = quote(p, others, false).net();
        BigInteger base = rangeCost(p, self.curve(), self.current(), lo);
        if (fixed.add(base).compareTo(budget) > 0) return lo;
        while (lo < hi) {
            int mid = lo + (hi - lo + 1) / 2;
            BigInteger cost = fixed.add(rangeCost(p, self.curve(), self.current(), mid));
            if (cost.compareTo(budget) <= 0) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }
}

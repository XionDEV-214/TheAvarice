package com.eisiadev.theavarice.currency;

import java.math.BigInteger;

public final class WalletMath {

    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private WalletMath() {}

    public static BigInteger total(long[] counts, long[] values) {
        BigInteger sum = BigInteger.ZERO;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) sum = sum.add(BigInteger.valueOf(counts[i]).multiply(BigInteger.valueOf(values[i])));
        }
        return sum;
    }

    public static BigInteger[] breakdown(long[] values, BigInteger amount) {
        BigInteger[] out = new BigInteger[values.length];
        BigInteger rem = amount.max(BigInteger.ZERO);
        for (int i = 0; i < values.length; i++) {
            BigInteger v = BigInteger.valueOf(values[i]);
            BigInteger[] qr = rem.divideAndRemainder(v);
            out[i] = qr[0];
            rem = qr[1];
        }
        return out;
    }

    public static long[] deposit(long[] counts, long[] values, BigInteger amount) {
        long[] c = counts.clone();
        depositInto(c, values, amount, 0);
        return c;
    }

    private static void depositInto(long[] c, long[] values, BigInteger amount, int fromIndex) {
        BigInteger rem = amount.max(BigInteger.ZERO);
        for (int i = fromIndex; i < values.length && rem.signum() > 0; i++) {
            BigInteger v = BigInteger.valueOf(values[i]);
            BigInteger[] qr = rem.divideAndRemainder(v);
            if (qr[0].signum() > 0) {
                BigInteger sum = BigInteger.valueOf(c[i]).add(qr[0]);
                c[i] = sum.compareTo(LONG_MAX) > 0 ? Long.MAX_VALUE : sum.longValue();
            }
            rem = qr[1];
        }
    }

    public static long[] pay(long[] counts, long[] values, BigInteger cost) {
        if (cost.signum() <= 0) return counts.clone();
        if (total(counts, values).compareTo(cost) < 0) return null;

        long[] c = counts.clone();
        BigInteger rem = cost;
        for (int i = 0; i < c.length && rem.signum() > 0; i++) {
            if (c[i] <= 0) continue;
            BigInteger v = BigInteger.valueOf(values[i]);
            BigInteger take = rem.divide(v).min(BigInteger.valueOf(c[i]));
            if (take.signum() > 0) {
                c[i] -= take.longValue();
                rem = rem.subtract(take.multiply(v));
            }
        }
        if (rem.signum() > 0) {
            for (int i = c.length - 1; i >= 0; i--) {
                if (c[i] > 0 && BigInteger.valueOf(values[i]).compareTo(rem) >= 0) {
                    c[i]--;
                    BigInteger change = BigInteger.valueOf(values[i]).subtract(rem);
                    rem = BigInteger.ZERO;
                    depositInto(c, values, change, i + 1);
                    break;
                }
            }
            if (rem.signum() > 0) return null;
        }
        return c;
    }
}

package com.eisiadev.theavarice.enchant;

public record CostParams(double baseCost, double exponent, double steepness,
                         double refundRatio, double naturalRefundRatio,
                         double curseBaseCost, double[] rarityWeights,
                         double treasureMultiplier, int globalMaxLevel) {

    public static final CostParams DEFAULT = new CostParams(
            6750.0, 2.0, 1000.0, 0.9, 0.005, 100_000.0,
            new double[]{1.0, 2.0, 4.0, 8.0}, 2.0, CostMath.ABSOLUTE_MAX_LEVEL);

    public double weight(int rarityOrdinal, boolean treasure) {
        double w = rarityOrdinal >= 0 && rarityOrdinal < rarityWeights.length ? rarityWeights[rarityOrdinal] : 1.0;
        return treasure ? w * treasureMultiplier : w;
    }
}

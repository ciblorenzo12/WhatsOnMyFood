package com.example.myapplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PantryRiskScorer {
    public static final int RECOMMENDED_DAILY_CALORIES = 2000;

    private PantryRiskScorer() {
    }

    public static List<RiskItem> scoreProducts(List<Product> products) {
        List<RiskItem> items = new ArrayList<>();
        if (products == null) return items;
        for (Product product : products) {
            items.add(new RiskItem(product, null, aiRisk(product), userRisk(product), null, combinedRisk(product, null)));
        }
        Collections.sort(items, (left, right) -> Integer.compare(right.combinedRisk, left.combinedRisk));
        return items;
    }

    public static List<RiskItem> scoreProductDetails(List<ProductWithDetails> productDetails) {
        List<RiskItem> items = new ArrayList<>();
        if (productDetails == null) return items;
        for (ProductWithDetails details : productDetails) {
            if (details == null || details.product == null) continue;
            Integer calorieRisk = calorieRisk(details.nutriments);
            items.add(new RiskItem(
                    details.product,
                    details.nutriments,
                    aiRisk(details.product),
                    userRisk(details.product),
                    calorieRisk,
                    combinedRisk(details.product, calorieRisk)
            ));
        }
        Collections.sort(items, (left, right) -> Integer.compare(right.combinedRisk, left.combinedRisk));
        return items;
    }

    public static RiskStats stats(List<RiskItem> items) {
        if (items == null || items.isEmpty()) {
            return new RiskStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        int combinedTotal = 0;
        int aiTotal = 0;
        int userTotal = 0;
        int healthScoreTotal = 0;
        int calorieTotal = 0;
        int aiCount = 0;
        int userCount = 0;
        int healthScoreCount = 0;
        int calorieCount = 0;
        int highRiskCount = 0;
        for (RiskItem item : items) {
            combinedTotal += item.combinedRisk;
            if (item.aiRisk != null) {
                aiTotal += item.aiRisk;
                aiCount++;
            }
            if (item.product.healthScore != null) {
                healthScoreTotal += item.product.healthScore;
                healthScoreCount++;
            }
            if (item.userRisk > 0) {
                userTotal += item.userRisk;
                userCount++;
            }
            if (item.caloriesPer100g != null) {
                calorieTotal += Math.round(item.caloriesPer100g);
                calorieCount++;
            }
            if (item.combinedRisk >= 70) {
                highRiskCount++;
            }
        }
        return new RiskStats(
                items.size(),
                Math.round(combinedTotal / (float) items.size()),
                aiCount == 0 ? 0 : Math.round(aiTotal / (float) aiCount),
                userCount == 0 ? 0 : Math.round(userTotal / (float) userCount),
                healthScoreCount == 0 ? 0 : Math.round(healthScoreTotal / (float) healthScoreCount),
                calorieCount == 0 ? 0 : Math.round(calorieTotal / (float) calorieCount),
                calorieTotal,
                Math.round(calorieTotal * 100f / RECOMMENDED_DAILY_CALORIES),
                aiCount,
                userCount,
                healthScoreCount,
                calorieCount,
                highRiskCount
        );
    }

    private static Integer aiRisk(Product product) {
        if (product.healthScore == null) return null;
        return clamp(100 - product.healthScore);
    }

    private static int userRisk(Product product) {
        return clamp(product.userIngredientRiskScore == null ? 0 : product.userIngredientRiskScore);
    }

    private static Integer calorieRisk(Nutriments nutriments) {
        if (nutriments == null || nutriments.energy == null) return null;
        double calories = Math.max(0, nutriments.energy);
        if (calories <= 120) {
            return clamp((int) Math.round(calories / 120d * 20d));
        }
        if (calories <= 250) {
            return clamp(20 + (int) Math.round((calories - 120d) / 130d * 35d));
        }
        return clamp(55 + (int) Math.round(Math.min(45d, (calories - 250d) / 300d * 45d)));
    }

    private static int combinedRisk(Product product, Integer calorieRisk) {
        Integer aiRisk = aiRisk(product);
        int userRisk = userRisk(product);
        float total = 0f;
        float weight = 0f;
        if (aiRisk != null) {
            total += aiRisk * 0.55f;
            weight += 0.55f;
        }
        if (userRisk > 0) {
            total += userRisk * 0.25f;
            weight += 0.25f;
        }
        if (calorieRisk != null) {
            total += calorieRisk * 0.20f;
            weight += 0.20f;
        }
        return weight == 0f ? 0 : Math.round(total / weight);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public static final class RiskItem {
        public final Product product;
        public final Nutriments nutriments;
        public final Integer aiRisk;
        public final int userRisk;
        public final Integer calorieRisk;
        public final Double caloriesPer100g;
        public final int combinedRisk;

        RiskItem(Product product, Nutriments nutriments, Integer aiRisk, int userRisk, Integer calorieRisk, int combinedRisk) {
            this.product = product;
            this.nutriments = nutriments;
            this.aiRisk = aiRisk;
            this.userRisk = userRisk;
            this.calorieRisk = calorieRisk;
            this.caloriesPer100g = nutriments != null ? nutriments.energy : null;
            this.combinedRisk = combinedRisk;
        }
    }

    public static final class RiskStats {
        public final int itemCount;
        public final int averageCombinedRisk;
        public final int averageAiRisk;
        public final int averageUserRisk;
        public final int averageHealthScore;
        public final int averageCalories;
        public final int totalCalories;
        public final int dailyCaloriesPercent;
        public final int aiRatedCount;
        public final int userRatedCount;
        public final int healthScoreCount;
        public final int calorieCount;
        public final int highRiskCount;

        RiskStats(int itemCount, int averageCombinedRisk, int averageAiRisk, int averageUserRisk, int averageHealthScore, int averageCalories, int totalCalories, int dailyCaloriesPercent, int aiRatedCount, int userRatedCount, int healthScoreCount, int calorieCount, int highRiskCount) {
            this.itemCount = itemCount;
            this.averageCombinedRisk = averageCombinedRisk;
            this.averageAiRisk = averageAiRisk;
            this.averageUserRisk = averageUserRisk;
            this.averageHealthScore = averageHealthScore;
            this.averageCalories = averageCalories;
            this.totalCalories = totalCalories;
            this.dailyCaloriesPercent = dailyCaloriesPercent;
            this.aiRatedCount = aiRatedCount;
            this.userRatedCount = userRatedCount;
            this.healthScoreCount = healthScoreCount;
            this.calorieCount = calorieCount;
            this.highRiskCount = highRiskCount;
        }
    }
}

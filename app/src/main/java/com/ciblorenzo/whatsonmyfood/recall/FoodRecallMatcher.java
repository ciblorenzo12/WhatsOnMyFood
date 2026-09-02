package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative local matcher: identifiers outrank strong name-and-brand evidence. */
public final class FoodRecallMatcher {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(?:\\d[\\s-]*){8,14}");

    public FoodRecallCheckResult match(Product product, FoodRecallDataset dataset) {
        if (product == null || dataset == null) return FoodRecallCheckResult.noKnownMatch("");
        Candidate best = null;
        for (FoodRecallRecord record : dataset.records) {
            if (record == null || !record.isActive()) continue;
            Candidate candidate = score(product, record);
            if (candidate != null && (best == null || candidate.score > best.score)) {
                best = candidate;
            }
        }
        if (best == null) return FoodRecallCheckResult.noKnownMatch(dataset.sourceUpdatedAt);
        return best.confirmed
                ? FoodRecallCheckResult.confirmed(best.record, best.score, dataset.sourceUpdatedAt)
                : FoodRecallCheckResult.possible(best.record, best.score, dataset.sourceUpdatedAt);
    }

    private Candidate score(Product product, FoodRecallRecord record) {
        if (containsBarcode(product.barcode, record.productDescription + " " + record.codeInfo)) {
            return new Candidate(record, 100, true);
        }

        String identityText = FoodRecallQueryBuilder.normalize(
                record.productDescription + " " + record.recallingFirm
        );
        List<String> nameTokens = FoodRecallQueryBuilder.significantTokens(product.productName);
        if (nameTokens.isEmpty()) return null;
        Set<String> recordTokens = new HashSet<>(FoodRecallQueryBuilder.significantTokens(identityText));
        int overlap = 0;
        for (String token : nameTokens) {
            if (recordTokens.contains(token)) overlap++;
        }
        double nameRatio = overlap / (double) nameTokens.size();
        boolean brandMatch = hasBrandMatch(product.brands, identityText);
        boolean quantityMatch = hasQuantityMatch(product.quantity, record.productDescription);

        if (overlap >= 2 && nameRatio >= 0.75 && brandMatch) {
            return new Candidate(record, 90 + (quantityMatch ? 5 : 0), true);
        }
        if ((overlap >= 2 && nameRatio >= 0.5) || (brandMatch && overlap >= 1)) {
            int score = 40 + overlap * 10 + (brandMatch ? 20 : 0) + (quantityMatch ? 5 : 0);
            return new Candidate(record, score, false);
        }
        return null;
    }

    static boolean containsBarcode(String barcode, String recordText) {
        String expected = digits(barcode);
        if (expected.length() < 8) return false;
        Matcher matcher = IDENTIFIER_PATTERN.matcher(recordText == null ? "" : recordText);
        while (matcher.find()) {
            String candidate = digits(matcher.group());
            if (sameIdentifier(expected, candidate)) return true;
        }
        return false;
    }

    private static boolean sameIdentifier(String first, String second) {
        if (first.equals(second)) return true;
        return trimLeadingZero(first).equals(trimLeadingZero(second));
    }

    private static String trimLeadingZero(String value) {
        return value.startsWith("0") ? value.substring(1) : value;
    }

    private static boolean hasBrandMatch(String brand, String identityText) {
        List<String> brandTokens = FoodRecallQueryBuilder.significantTokens(brand);
        if (brandTokens.isEmpty()) return false;
        int found = 0;
        for (String token : brandTokens) {
            if (identityText.contains(token)) found++;
        }
        return found >= Math.max(1, (int) Math.ceil(brandTokens.size() * 0.75));
    }

    private static boolean hasQuantityMatch(String quantity, String description) {
        String normalizedQuantity = FoodRecallQueryBuilder.normalize(quantity).replace(" ", "");
        String normalizedDescription = FoodRecallQueryBuilder.normalize(description).replace(" ", "");
        return normalizedQuantity.length() >= 2 && normalizedDescription.contains(normalizedQuantity);
    }

    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D+", "");
    }

    private static final class Candidate {
        final FoodRecallRecord record;
        final int score;
        final boolean confirmed;

        Candidate(FoodRecallRecord record, int score, boolean confirmed) {
            this.record = record;
            this.score = score;
            this.confirmed = confirmed;
        }
    }
}

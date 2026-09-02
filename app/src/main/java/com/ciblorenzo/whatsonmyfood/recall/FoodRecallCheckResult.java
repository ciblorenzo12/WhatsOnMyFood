package com.ciblorenzo.whatsonmyfood.recall;

public final class FoodRecallCheckResult {
    public final FoodRecallState state;
    public final FoodRecallRecord record;
    public final int confidenceScore;
    public final String sourceUpdatedAt;

    private FoodRecallCheckResult(
            FoodRecallState state,
            FoodRecallRecord record,
            int confidenceScore,
            String sourceUpdatedAt
    ) {
        this.state = state;
        this.record = record;
        this.confidenceScore = confidenceScore;
        this.sourceUpdatedAt = sourceUpdatedAt == null ? "" : sourceUpdatedAt;
    }

    public static FoodRecallCheckResult noKnownMatch(String sourceUpdatedAt) {
        return new FoodRecallCheckResult(FoodRecallState.NO_KNOWN_MATCH, null, 0, sourceUpdatedAt);
    }

    public static FoodRecallCheckResult possible(FoodRecallRecord record, int score, String sourceUpdatedAt) {
        return new FoodRecallCheckResult(FoodRecallState.POSSIBLE_MATCH, record, score, sourceUpdatedAt);
    }

    public static FoodRecallCheckResult confirmed(FoodRecallRecord record, int score, String sourceUpdatedAt) {
        return new FoodRecallCheckResult(FoodRecallState.CONFIRMED_MATCH, record, score, sourceUpdatedAt);
    }
}

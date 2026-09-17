package com.ciblorenzo.whatsonmyfood.recall;

/** Normalized fields from one official FDA food enforcement record. */
public final class FoodRecallRecord {
    public final String recallNumber;
    public final String productDescription;
    public final String recallingFirm;
    public final String classification;
    public final String reasonForRecall;
    public final String codeInfo;
    public final String reportDate;
    public final String status;

    public FoodRecallRecord(
            String recallNumber,
            String productDescription,
            String recallingFirm,
            String classification,
            String reasonForRecall,
            String codeInfo,
            String reportDate,
            String status
    ) {
        this.recallNumber = safe(recallNumber);
        this.productDescription = safe(productDescription);
        this.recallingFirm = safe(recallingFirm);
        this.classification = safe(classification);
        this.reasonForRecall = safe(reasonForRecall);
        this.codeInfo = safe(codeInfo);
        this.reportDate = safe(reportDate);
        this.status = safe(status);
    }

    public boolean isActive() {
        String normalized = status.trim().toLowerCase();
        return !normalized.equals("terminated") && !normalized.equals("completed");
    }

    public String officialUrl() {
        // Link to the exact official FDA record rather than an unrelated general landing page.
        return new okhttp3.HttpUrl.Builder().scheme("https").host("api.fda.gov")
                .addPathSegments("food/enforcement.json")
                .addQueryParameter("search", "recall_number:\"" + recallNumber.replace("\"", "") + "\"")
                .addQueryParameter("limit", "1").build().toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

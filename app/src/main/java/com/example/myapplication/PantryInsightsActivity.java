package com.example.myapplication;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.analysis.AnalysisResult;
import com.example.myapplication.analysis.ProductAnalysisReport;
import com.example.myapplication.analysis.rules.RuleEngine;
import com.example.myapplication.utils.GlassMotion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PantryInsightsActivity extends BaseActivity {
    private AppDatabase db;
    private ExecutorService executorService;
    private PantryRiskChartView chartView;
    private PantryHealthProfileView healthProfileView;
    private TextView combinedTextView;
    private TextView aiTextView;
    private TextView userTextView;
    private TextView highRiskTextView;
    private TextView emptyTextView;
    private TextView insightSummaryTextView;
    private TextView productIngredientBreakdownTextView;
    private TextView calorieBreakdownTextView;
    private ImageView processedGraphImageView;
    private FirebaseUser currentUser;
    private RuleEngine ruleEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry_insights);

        Toolbar toolbar = findViewById(R.id.pantry_insights_toolbar);
        setSupportActionBar(toolbar);
        GlassMotion.enter(toolbar, 0L);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        ruleEngine = new RuleEngine();

        chartView = findViewById(R.id.pantry_risk_chart);
        healthProfileView = findViewById(R.id.pantry_health_profile_view);
        combinedTextView = findViewById(R.id.combined_risk_text_view);
        aiTextView = findViewById(R.id.ai_risk_text_view);
        userTextView = findViewById(R.id.user_risk_text_view);
        highRiskTextView = findViewById(R.id.high_risk_text_view);
        emptyTextView = findViewById(R.id.empty_scores_text_view);
        insightSummaryTextView = findViewById(R.id.insight_summary_text_view);
        productIngredientBreakdownTextView = findViewById(R.id.product_ingredient_breakdown_text_view);
        calorieBreakdownTextView = findViewById(R.id.calorie_breakdown_text_view);
        processedGraphImageView = findViewById(R.id.processed_graph_image_view);
        loadRiskData();
    }

    private void loadRiskData() {
        if (currentUser == null) return;
        executorService.execute(() -> {
            List<ProductWithDetails> products = db.productDao().getPantryProductsWithDetails(currentUser.getUid());
            List<ProductAnalysisReport> reports = new ArrayList<>();
            for (ProductWithDetails product : products) {
                ProductAnalysisReport report = ruleEngine.analyze(product);
                reports.add(report);
                if (product != null && product.product != null && report != null) {
                    product.product.healthScore = report.getOverallScore();
                }
            }
            List<PantryRiskScorer.RiskItem> items = PantryRiskScorer.scoreProductDetails(products);
            PantryRiskScorer.RiskStats stats = PantryRiskScorer.stats(items);
            String breakdown = buildProductBreakdown(products, reports, items);
            String insightSummary = buildInsightSummary(items, stats);
            String calorieBreakdown = buildCalorieBreakdown(items, stats);
            runOnUiThread(() -> bindStats(items, stats, breakdown, insightSummary, calorieBreakdown));
        });
    }

    private void bindStats(List<PantryRiskScorer.RiskItem> items, PantryRiskScorer.RiskStats stats, String breakdown, String insightSummary, String calorieBreakdown) {
        chartView.setItems(items);
        healthProfileView.setStats(stats);
        combinedTextView.setText(String.valueOf(stats.averageCombinedRisk));
        aiTextView.setText(stats.healthScoreCount == 0 ? "--" : String.valueOf(stats.averageHealthScore));
        userTextView.setText(stats.userRatedCount == 0 ? "--" : String.valueOf(stats.averageUserRisk));
        highRiskTextView.setText(stats.calorieCount == 0 ? "--" : stats.dailyCaloriesPercent + "%");
        processedGraphImageView.setImageBitmap(PantryRiskBitmapRenderer.render(this, items, stats));
        insightSummaryTextView.setText(Html.fromHtml(insightSummary, Html.FROM_HTML_MODE_COMPACT));
        productIngredientBreakdownTextView.setText(Html.fromHtml(breakdown, Html.FROM_HTML_MODE_COMPACT));
        calorieBreakdownTextView.setText(Html.fromHtml(calorieBreakdown, Html.FROM_HTML_MODE_COMPACT));
        boolean hasScores = stats.healthScoreCount > 0 || stats.userRatedCount > 0 || stats.calorieCount > 0;
        emptyTextView.setVisibility(hasScores ? View.GONE : View.VISIBLE);
    }

    private String buildInsightSummary(List<PantryRiskScorer.RiskItem> items, PantryRiskScorer.RiskStats stats) {
        if (items == null || items.isEmpty()) {
            return "<b>What this graph is showing</b><br>Add products to compare health score, user concern, calories, and combined pantry risk.";
        }

        PantryRiskScorer.RiskItem highest = items.get(0);
        String highestName = highest.product == null || highest.product.productName == null
                ? "Pantry item"
                : highest.product.productName;
        int low = 0;
        int moderate = 0;
        int high = 0;
        for (PantryRiskScorer.RiskItem item : items) {
            if (item.combinedRisk >= 70) {
                high++;
            } else if (item.combinedRisk >= 40) {
                moderate++;
            } else {
                low++;
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<b>What this graph is showing</b><br>");
        builder.append("Highest risk product: <b>").append(escape(highestName)).append("</b> at ");
        builder.append(highest.combinedRisk).append("/100.<br>");
        builder.append("Risk mix: <font color='#059669'>").append(low).append(" low</font>, ");
        builder.append("<font color='#F59E0B'>").append(moderate).append(" moderate</font>, ");
        builder.append("<font color='#EF4444'>").append(high).append(" high</font>.<br>");
        builder.append("Pantry calories: <b>").append(stats.totalCalories).append(" kcal</b>, ");
        builder.append(stats.dailyCaloriesPercent).append("% of a ");
        builder.append(PantryRiskScorer.RECOMMENDED_DAILY_CALORIES).append(" kcal daily target.");
        return builder.toString();
    }

    private String buildCalorieBreakdown(List<PantryRiskScorer.RiskItem> items, PantryRiskScorer.RiskStats stats) {
        if (items == null || items.isEmpty() || stats.calorieCount == 0 || stats.totalCalories <= 0) {
            return "<b>Calorie contribution</b><br>No calorie data is available yet.";
        }

        List<PantryRiskScorer.RiskItem> calorieItems = new ArrayList<>(items);
        calorieItems.sort((left, right) -> Double.compare(
                right.caloriesPer100g == null ? 0 : right.caloriesPer100g,
                left.caloriesPer100g == null ? 0 : left.caloriesPer100g
        ));

        StringBuilder builder = new StringBuilder();
        builder.append("<b>Calorie contribution</b><br>");
        int count = 0;
        for (PantryRiskScorer.RiskItem item : calorieItems) {
            if (item.caloriesPer100g == null || item.product == null) continue;
            int calories = (int) Math.round(item.caloriesPer100g);
            int percent = Math.round(calories * 100f / stats.totalCalories);
            builder.append(escape(item.product.productName == null ? "Pantry item" : item.product.productName));
            builder.append(": <b>").append(calories).append(" kcal</b> per 100g, ");
            builder.append(percent).append("% of pantry calories<br>");
            count++;
            if (count >= 5) break;
        }
        return builder.toString();
    }

    private String buildProductBreakdown(List<ProductWithDetails> products, List<ProductAnalysisReport> reports, List<PantryRiskScorer.RiskItem> items) {
        if (products == null || products.isEmpty()) {
            return "<b>Pantry product scores</b><br>Add products to your pantry to see healthy and concern ingredients.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<b>Pantry product scores</b><br>");
        for (int i = 0; i < products.size(); i++) {
            ProductWithDetails product = products.get(i);
            if (product == null || product.product == null) continue;

            ProductAnalysisReport report = i < reports.size() ? reports.get(i) : null;
            int healthScore = report != null ? report.getOverallScore() : (product.product.healthScore != null ? product.product.healthScore : 0);
            int riskScore = findRiskScore(items, product.product.barcode);
            builder.append("<br><b>").append(escape(product.product.productName != null ? product.product.productName : "Pantry item")).append("</b>");
            builder.append(" - Score ").append(healthScore).append("/100");
            builder.append(" | Risk ").append(riskScore).append("/100<br>");
            builder.append("<font color='#059669'>Healthy ingredients:</font> ");
            builder.append(escape(joinIngredients(product, true, report))).append("<br>");
            builder.append("<font color='#EF4444'>Concern ingredients:</font> ");
            builder.append(escape(joinIngredients(product, false, report))).append("<br>");
            builder.append("<font color='#7C4DFF'>Classifications:</font> ");
            builder.append(escape(joinClassifications(report))).append("<br>");
        }
        return builder.toString();
    }

    private int findRiskScore(List<PantryRiskScorer.RiskItem> items, String barcode) {
        if (items == null || barcode == null) return 0;
        for (PantryRiskScorer.RiskItem item : items) {
            if (item.product != null && barcode.equals(item.product.barcode)) {
                return item.combinedRisk;
            }
        }
        return 0;
    }

    private String joinIngredients(ProductWithDetails product, boolean healthy, ProductAnalysisReport report) {
        List<String> values = new ArrayList<>();
        if (report != null && report.getResults() != null) {
            for (AnalysisResult result : report.getResults()) {
                if (result == null) continue;
                boolean isHealthy = result.getLevel() == AnalysisResult.WarningLevel.POSITIVE;
                boolean isConcern = result.getLevel() == AnalysisResult.WarningLevel.WARNING
                        || result.getLevel() == AnalysisResult.WarningLevel.SEVERE;
                if (!healthy && isClassification(result)) {
                    continue;
                }
                if ((healthy && isHealthy) || (!healthy && isConcern)) {
                    String value = result.getTriggeringIngredient();
                    if (value == null || value.trim().isEmpty()) value = result.getMessage();
                    addUnique(values, clean(value));
                }
            }
        }

        if (healthy && values.isEmpty() && product.ingredients != null) {
            for (Ingredient ingredient : product.ingredients) {
                if (ingredient == null || ingredient.text == null) continue;
                String normalized = ingredient.text.toLowerCase(Locale.US);
                if (normalized.contains("organic")
                        || normalized.contains("whole")
                        || normalized.contains("fiber")
                        || normalized.contains("protein")
                        || normalized.contains("fruit")
                        || normalized.contains("vegetable")) {
                    addUnique(values, clean(ingredient.text));
                }
                if (values.size() >= 4) break;
            }
        }

        return values.isEmpty() ? "None detected" : join(values, 4);
    }

    private String joinClassifications(ProductAnalysisReport report) {
        List<String> values = new ArrayList<>();
        if (report != null && report.getResults() != null) {
            for (AnalysisResult result : report.getResults()) {
                if (isClassification(result)) {
                    addUnique(values, clean(result.getMessage()));
                }
            }
        }
        return values.isEmpty() ? "None detected" : join(values, 4);
    }

    private boolean isClassification(AnalysisResult result) {
        if (result == null) return false;
        String text = ((result.getMessage() != null ? result.getMessage() : "") + " "
                + (result.getTriggeringIngredient() != null ? result.getTriggeringIngredient() : ""))
                .toLowerCase(Locale.US);
        return text.contains("nova")
                || text.contains("nutri-score")
                || text.contains("ecoscore")
                || text.contains("eco-score");
    }

    private void addUnique(List<String> values, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (!values.contains(value)) values.add(value);
    }

    private String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String join(List<String> values, int max) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.min(values.size(), max); i++) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(values.get(i));
        }
        if (values.size() > max) builder.append(", +").append(values.size() - max).append(" more");
        return builder.toString();
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

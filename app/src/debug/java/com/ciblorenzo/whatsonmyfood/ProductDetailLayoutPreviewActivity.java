package com.ciblorenzo.whatsonmyfood;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;
import com.ciblorenzo.whatsonmyfood.analysis.ProductAnalysisReport;
import com.ciblorenzo.whatsonmyfood.analysis.ProductFindingsDisplay;
import com.ciblorenzo.whatsonmyfood.analysis.ProductFindingsViewBinder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Debug-only screen that renders the product-detail fragment layout with safe sample data. */
public class ProductDetailLayoutPreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_product_details);

        findViewById(R.id.loading_overlay).setVisibility(View.GONE);
        ((ImageView) findViewById(R.id.product_image_view)).setImageResource(R.drawable.m4_preview_product);
        setText(R.id.product_name_text_view, "Whole Grain Oat Cereal");
        setText(R.id.product_brand_text_view, "Sample Market Foods");

        bindSourcePreview();

        setText(R.id.health_score_text_view, "Good choice");
        bindScore(R.id.nutriscore_text_view, "Nutri-Score: B", R.color.nutriscore_b);
        bindScore(R.id.nova_text_view, "NOVA: 2", R.color.nova_2);
        bindScore(R.id.ecoscore_text_view, "Eco-Score: B", R.color.nutriscore_b);

        RecyclerView findings = findViewById(R.id.analysis_recycler_view);
        findings.setLayoutManager(new LinearLayoutManager(this));
        findings.setNestedScrollingEnabled(false);
        List<AnalysisResult> sampleFindings = Arrays.asList(
                new AnalysisResult(
                        "Whole grain oats",
                        AnalysisResult.WarningLevel.POSITIVE,
                        -5,
                        "whole grain oats",
                        "Whole grains can contribute dietary fiber and important nutrients."
                ),
                new AnalysisResult(
                        "Added sugar",
                        AnalysisResult.WarningLevel.WARNING,
                        8,
                        "sugar",
                        "Compare the serving size and added-sugar amount with your shopping priorities."
                ),
                new AnalysisResult(
                        "Allergen information",
                        AnalysisResult.WarningLevel.INFO,
                        0,
                        "oats",
                        "Check the package statement if you manage a food allergy."
                ),
                new AnalysisResult(
                        "Added sugar",
                        AnalysisResult.WarningLevel.WARNING,
                        8,
                        "sugar",
                        "Duplicate input included to verify that only one result card is displayed."
                )
        );
        bindFindingsPreview(findings, sampleFindings);

        findViewById(R.id.ai_summary_container).setVisibility(View.VISIBLE);
        if ("ai_unavailable".equals(getIntent().getStringExtra("source_scenario"))) {
            setText(R.id.ai_summary_text_view, getString(R.string.bitwise_retry_explanation));
        } else {
            setText(R.id.ai_summary_text_view,
                    "This cereal starts with whole grain oats and has a moderate added-sugar concern. "
                            + "Use the serving information below to compare it with similar products.");
        }
        findViewById(R.id.ai_sources_divider).setVisibility(View.VISIBLE);
        findViewById(R.id.ai_sources_label).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.ai_sources_text_view)).setText(buildSourcePreview());
        ((TextView) findViewById(R.id.ai_sources_text_view)).setMovementMethod(
                android.text.method.LinkMovementMethod.getInstance()
        );
        findViewById(R.id.ai_sources_text_view).setVisibility(View.VISIBLE);

        setText(R.id.ingredients_text_view,
                "Whole grain oats, corn starch, sugar, salt, tripotassium phosphate, vitamin E");
        setText(R.id.serving_size_text_view, "1 cup (36 g)");
        addNutritionRow("Calories", "140 kcal");
        addNutritionRow("Protein", "5 g");
        addNutritionRow("Added sugar", "4 g");
        addNutritionRow("Sodium", "190 mg");
        setText(R.id.categories_text_view, "Breakfast cereals, whole grain foods");
        setText(R.id.packaging_text_view, "Recyclable cardboard box");
        setText(R.id.labels_text_view, "Whole grain certified");

        bindPantryActionPreview();
        if (getIntent().getBooleanExtra("pantry_focus", false)) {
            findViewById(R.id.nested_scroll_view).post(() ->
                    ((androidx.core.widget.NestedScrollView) findViewById(R.id.nested_scroll_view))
                            .fullScroll(View.FOCUS_DOWN)
            );
        }
    }

    private void bindSourcePreview() {
        String scenario = getIntent().getStringExtra("source_scenario");
        List<ProductRepository.SourceStatus> statuses;
        if ("recovered".equals(scenario)) {
            statuses = Arrays.asList(
                    ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE,
                    ProductRepository.SourceStatus.INGREDIENTS_RECOVERED_FROM_LABEL_OR_SUPPORTING_SERVICE
            );
        } else if ("stale".equals(scenario)) {
            statuses = Collections.singletonList(
                    ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED
            );
        } else if ("offline".equals(scenario)) {
            statuses = Arrays.asList(
                    ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT,
                    ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED
            );
        } else if ("ai_unavailable".equals(scenario)) {
            statuses = Arrays.asList(
                    ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE,
                    ProductRepository.SourceStatus.AI_EXPLANATION_UNAVAILABLE
            );
        } else {
            statuses = Collections.singletonList(
                    ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE
            );
        }

        SourceStatusViewBinder.bind(
                this,
                findViewById(R.id.source_status_container),
                findViewById(R.id.source_status_indicator_text_view),
                findViewById(R.id.source_status_text_view),
                findViewById(R.id.source_status_interpretation_text_view),
                statuses
        );
    }

    private CharSequence buildSourcePreview() {
        JSONArray sources = new JSONArray();
        try {
            sources.put(new JSONObject()
                    .put("name", "FDA Nutrition Facts guidance")
                    .put("url", "https://www.fda.gov/food/nutrition-facts-label/how-understand-and-use-nutrition-facts-label")
                    .put("search_query", "nutrition facts label serving size"));
            sources.put(new JSONObject()
                    .put("name", "WHO healthy diet guidance")
                    .put("url", "https://www.who.int/news-room/fact-sheets/detail/healthy-diet")
                    .put("search_query", "healthy diet nutrition"));
        } catch (Exception ignored) {
            return "";
        }
        return ScientificSourceTextBuilder.build(this, sources);
    }

    private void bindPantryActionPreview() {
        String state = getIntent().getStringExtra("pantry_state");
        PantryActionViewBinder.State displayState;
        if ("available".equals(state)) {
            displayState = PantryActionViewBinder.State.AVAILABLE_TO_SAVE;
        } else if ("saving".equals(state)) {
            displayState = PantryActionViewBinder.State.SAVING;
        } else if ("removing".equals(state)) {
            displayState = PantryActionViewBinder.State.REMOVING;
        } else {
            displayState = PantryActionViewBinder.State.SAVED;
        }
        PantryActionViewBinder.bind(
                findViewById(R.id.add_to_pantry_button),
                findViewById(R.id.remove_from_pantry_button),
                displayState
        );
    }

    private void bindFindingsPreview(RecyclerView findings, List<AnalysisResult> sampleFindings) {
        String scenario = getIntent().getStringExtra("findings_scenario");
        ProductFindingsDisplay display;
        if ("empty".equals(scenario)) {
            display = ProductFindingsDisplay.fromReport(
                    new ProductAnalysisReport(100, Collections.emptyList()),
                    true
            );
        } else if ("missing".equals(scenario)) {
            display = ProductFindingsDisplay.fromReport(null, false);
        } else if ("unavailable".equals(scenario)) {
            display = ProductFindingsDisplay.fromReport(null, true);
        } else {
            display = ProductFindingsDisplay.fromReport(
                    new ProductAnalysisReport(79, sampleFindings),
                    true
            );
        }

        ProductFindingsViewBinder.bind(
                findings,
                findViewById(R.id.findings_empty_state_text_view),
                display
        );
    }

    private void bindScore(int viewId, String text, int colorId) {
        TextView score = findViewById(viewId);
        score.setText(text);
        score.setVisibility(View.VISIBLE);
        score.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorId)));
    }

    private void addNutritionRow(String label, String value) {
        TableLayout table = findViewById(R.id.nutrition_facts_table);
        TableRow row = new TableRow(this);
        TextView nameView = new TextView(this);
        TextView valueView = new TextView(this);
        nameView.setText(label);
        valueView.setText(value);
        nameView.setPadding(8, 6, 8, 6);
        valueView.setPadding(8, 6, 8, 6);
        valueView.setGravity(Gravity.END);
        row.addView(nameView);
        row.addView(valueView);
        table.addView(row);
    }

    private void setText(int viewId, String value) {
        ((TextView) findViewById(viewId)).setText(value);
    }
}

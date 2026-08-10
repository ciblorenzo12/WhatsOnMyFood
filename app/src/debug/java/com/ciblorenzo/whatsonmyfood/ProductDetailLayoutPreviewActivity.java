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
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultAdapter;

import java.util.Arrays;

/** Debug-only screen that renders the real product-detail layout with safe sample data. */
public class ProductDetailLayoutPreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        findViewById(R.id.loading_overlay).setVisibility(View.GONE);
        ((ImageView) findViewById(R.id.product_image_view)).setImageResource(R.drawable.m4_preview_product);
        setText(R.id.product_name_text_view, "Whole Grain Oat Cereal");
        setText(R.id.product_brand_text_view, "Sample Market Foods");

        findViewById(R.id.source_status_container).setVisibility(View.VISIBLE);
        setText(R.id.source_status_text_view,
                "Updated from product database. Ingredients verified from the package label.");

        setText(R.id.health_score_text_view, "Good choice");
        bindScore(R.id.nutriscore_text_view, "Nutri-Score: B", R.color.nutriscore_b);
        bindScore(R.id.nova_text_view, "NOVA: 2", R.color.nova_2);
        bindScore(R.id.ecoscore_text_view, "Eco-Score: B", R.color.nutriscore_b);

        RecyclerView findings = findViewById(R.id.analysis_recycler_view);
        findings.setLayoutManager(new LinearLayoutManager(this));
        findings.setNestedScrollingEnabled(false);
        findings.setAdapter(new AnalysisResultAdapter(Arrays.asList(
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
                )
        )));

        findViewById(R.id.ai_summary_container).setVisibility(View.VISIBLE);
        setText(R.id.ai_summary_text_view,
                "This cereal starts with whole grain oats and has a moderate added-sugar concern. "
                        + "Use the serving information below to compare it with similar products.");
        findViewById(R.id.ai_sources_divider).setVisibility(View.VISIBLE);
        findViewById(R.id.ai_sources_label).setVisibility(View.VISIBLE);
        setText(R.id.ai_sources_text_view, "• FDA Nutrition Facts guidance\n• WHO healthy diet guidance");
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

        Button removeButton = findViewById(R.id.remove_from_pantry_button);
        removeButton.setVisibility(View.VISIBLE);
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

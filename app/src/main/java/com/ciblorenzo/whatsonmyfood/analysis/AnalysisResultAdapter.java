package com.ciblorenzo.whatsonmyfood.analysis;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.R;
import com.ciblorenzo.whatsonmyfood.WebViewActivity;
import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;

import java.util.List;

public class AnalysisResultAdapter extends RecyclerView.Adapter<AnalysisResultAdapter.ViewHolder> {

    private final List<AnalysisResult> results;

    public AnalysisResultAdapter(List<AnalysisResult> results) {
        this.results = AnalysisResultDeduplicator.deduplicate(results);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.analysis_warning_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnalysisResult result = results.get(position);
        holder.warningMessage.setText(result.getMessage());
        GlassMotion.enter(holder.itemView, Math.min(position * 25L, 160L));

        AnalysisResult.WarningLevel level = result.getLevel() != null ? result.getLevel() : AnalysisResult.WarningLevel.INFO;
        String displayExplanation = displayExplanation(result);
        String compactExplanation = compactExplanation(displayExplanation);
        switch (level) {
            case POSITIVE:
                holder.severityChip.setText("GOOD");
                tintChip(holder.severityChip, "#16A34A");
                break;
            case INFO:
                holder.severityChip.setText("INFO");
                tintChip(holder.severityChip, "#2563EB");
                break;
            case WARNING:
                holder.severityChip.setText("WATCH");
                tintChip(holder.severityChip, "#D97706");
                break;
            case SEVERE:
                holder.severityChip.setText("AVOID");
                tintChip(holder.severityChip, "#DC2626");
                break;
        }
        if (compactExplanation != null) {
            holder.warningExplanation.setText(compactExplanation);
            holder.warningExplanation.setVisibility(View.VISIBLE);
        } else {
            holder.warningExplanation.setText("");
            holder.warningExplanation.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle(result.getMessage())
                    .setMessage(displayExplanation)
                    .setPositiveButton("OK", null)
                    .show();
        });

        if (result.getSourceUrl() != null && !result.getSourceUrl().isEmpty()) {
            holder.viewSourceButton.setVisibility(View.VISIBLE);
            holder.viewSourceButton.setOnClickListener(v -> {
                LinkHandler.openLink(v.getContext(), result.getSourceUrl(), "Verification: " + result.getMessage(), result.getVisualQuote());
            });
        } else {
            holder.viewSourceButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    private void tintChip(TextView chip, String color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor(color));
        drawable.setCornerRadius(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                10,
                chip.getResources().getDisplayMetrics()
        ));
        chip.setBackground(drawable);
    }

    private String compactExplanation(String explanation) {
        if (explanation == null || explanation.trim().isEmpty()) return null;
        String cleaned = explanation.trim()
                .replace("âŒ", "")
                .replace("âš ï¸", "")
                .replace("âœ…", "")
                .trim();
        int sentenceEnd = cleaned.indexOf(". ");
        if (sentenceEnd > 0) {
            cleaned = cleaned.substring(0, sentenceEnd + 1);
        }
        return cleaned;
    }

    private String displayExplanation(AnalysisResult result) {
        String message = result.getMessage() == null ? "" : result.getMessage();
        String allergens = result.getTriggeringIngredient() == null
                ? ""
                : result.getTriggeringIngredient().trim();
        if (!allergens.isEmpty() && message.startsWith("Allergen statement: Contains")) {
            return "Contains: " + allergens
                    + " - informational only (0 points). "
                    + result.getExplanation();
        }
        if (!allergens.isEmpty() && message.startsWith("Allergen advisory: May contain")) {
            return "May contain: " + allergens
                    + " - informational only (0 points). "
                    + result.getExplanation();
        }
        return result.getScoringExplanation();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView severityChip;
        TextView warningMessage;
        TextView warningExplanation;
        Button viewSourceButton;

        ViewHolder(View view) {
            super(view);
            severityChip = view.findViewById(R.id.severity_chip);
            warningMessage = view.findViewById(R.id.warning_message);
            warningExplanation = view.findViewById(R.id.warning_explanation);
            viewSourceButton = view.findViewById(R.id.view_source_button);
            GlassMotion.attachPress(view);
        }
    }
}

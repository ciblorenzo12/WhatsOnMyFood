package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;

import org.json.JSONArray;
import org.json.JSONObject;

/** Builds one consistent, clickable scientific-source list for every result screen. */
public final class ScientificSourceTextBuilder {

    private ScientificSourceTextBuilder() {
    }

    public static CharSequence build(Context context, JSONArray sources) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (context == null || sources == null) return builder;

        boolean hasSource = false;
        for (int i = 0; i < sources.length(); i++) {
            JSONObject source = sources.optJSONObject(i);
            if (source == null) continue;
            String name = source.optString("name", "Source").trim();
            String url = source.optString("url", "").trim();
            if (url.isEmpty()) continue;

            if (!hasSource) {
                builder.append(context.getString(R.string.source_quality_disclaimer)).append("\n\n");
                hasSource = true;
            }

            String displayName = name.isEmpty() ? context.getString(R.string.source_fallback_name) : name;
            String visualQuote = source.optString("visual_quote", "");
            int start = builder.length();
            builder.append("\u2022 ").append(displayName);
            int end = builder.length();
            builder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    LinkHandler.openLink(context, url, displayName, visualQuote);
                }
            }, start + 2, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary)),
                    start + 2,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            SourceReliabilityEvaluator.Rating rating = rating(source, displayName, url);
            builder.append("\n  ").append(context.getString(
                    R.string.source_quality_score,
                    rating.score,
                    levelLabel(context, rating.level)
            )).append("\n");
        }
        return builder;
    }

    private static SourceReliabilityEvaluator.Rating rating(JSONObject source, String name, String url) {
        JSONObject verification = source.optJSONObject("verification");
        int serverScore = verification != null ? verification.optInt("score", -1) : -1;
        if (serverScore >= 0 && serverScore <= 100) {
            return SourceReliabilityEvaluator.fromServerScore(serverScore);
        }
        return SourceReliabilityEvaluator.evaluate(
                name,
                url,
                source.optString("search_query", "")
        );
    }

    private static String levelLabel(Context context, SourceReliabilityEvaluator.Level level) {
        switch (level) {
            case VERY_STRONG:
                return context.getString(R.string.source_quality_very_strong);
            case STRONG:
                return context.getString(R.string.source_quality_strong);
            case MODERATE:
                return context.getString(R.string.source_quality_moderate);
            default:
                return context.getString(R.string.source_quality_limited);
        }
    }
}

package com.example.myapplication.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import com.example.myapplication.WebViewActivity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Advanced Link Handler with Chrome "Scroll-to-Text" Highlighting.
 */
public class LinkHandler {
    private static final String TAG = "LinkHandler";

    /**
     * Opens a URL with an optional "Text Fragment" for automatic highlighting.
     * Uses the format: URL#:~:text=ENCODED_TEXT
     */
    public static void openLink(Context context, String url, String title, String highlightText) {
        if (url == null || url.isEmpty()) return;

        String finalUrl = url;
        try {
            // If we have a specific quote, use Chrome's Text Fragment feature to highlight it on the page
            if (highlightText != null && !highlightText.isEmpty()) {
                // Truncate to a manageable length (max 10 words) for the fragment
                String[] words = highlightText.split("\\s+");
                StringBuilder fragment = new StringBuilder();
                for (int i = 0; i < Math.min(words.length, 10); i++) {
                    fragment.append(words[i]).append(" ");
                }
                String encodedFragment = URLEncoder.encode(fragment.toString().trim(), StandardCharsets.UTF_8.toString());

                // Construct the Scroll-to-Text URL
                if (url.contains("#")) {
                    finalUrl = url + ":~:text=" + encodedFragment;
                } else {
                    finalUrl = url + "#:~:text=" + encodedFragment;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error encoding text fragment", e);
        }

        String displayTitle = title != null && !title.trim().isEmpty() ? title : "Scientific Source";
        String host = Uri.parse(url).getHost();
        String message = "This source is a website outside YourHealthyPantry"
                + (host != null ? " from " + host : "")
                + ". It will open in the in-app web viewer.";

        final String sourceUrl = finalUrl;
        new AlertDialog.Builder(context)
                .setTitle("Open External Source?")
                .setMessage(message)
                .setPositiveButton("Open Source", (dialog, which) -> openWebViewer(context, sourceUrl, displayTitle))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void openWebViewer(Context context, String url, String title) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("source_name", title);
        context.startActivity(intent);
    }
}

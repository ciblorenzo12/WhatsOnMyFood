package com.example.myapplication.analysis;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Direct On-Device Web Scraper for Scientific Sources.
 * Bypasses "dumb" server agents by reading the content directly.
 */
public class ScientificReader {
    private static final String TAG = "ScientificReader";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface ReaderCallback {
        void onContentRead(String content);
        void onError(Exception e);
    }

    /**
     * Reads a URL and extracts relevant scientific text.
     */
    public void readUrl(String url, ReaderCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Fetch the document with robust headers to prevent blocks
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                        .timeout(15000) // Increased timeout for scientific PDFs
                        .followRedirects(true)
                        .get();

                // 2. Clean the document: remove scripts, styles, nav, and footer
                doc.select("script, style, nav, footer, header, .sidebar, #comments").remove();

                // 3. Target main content areas (common in scientific sites)
                Element contentBody = doc.body();
                
                // Try to find specific article containers if they exist
                Elements articles = doc.select("article, .article, .main-content, #main-content, .content, [role=main]");
                if (!articles.isEmpty()) {
                    contentBody = articles.first();
                }

                // 4. Extract and clean the text
                String rawText = contentBody.text();
                
                // Optimized for speed: Capture only the most relevant first 1000 words
                // This is usually more than enough for the AI to find the safety verdict
                String processedText = rawText.length() > 4000 ? rawText.substring(0, 4000) : rawText;

                callback.onContentRead(processedText);
                Log.d(TAG, "Successfully read content from: " + url);

            } catch (IOException e) {
                Log.e(TAG, "Error reading URL: " + url, e);
                callback.onError(e);
            }
        });
    }
}

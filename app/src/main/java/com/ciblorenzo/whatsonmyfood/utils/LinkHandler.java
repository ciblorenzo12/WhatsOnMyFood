package com.ciblorenzo.whatsonmyfood.utils;

import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import com.ciblorenzo.whatsonmyfood.WebViewActivity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Advanced Link Handler with Chrome "Scroll-to-Text" Highlighting.
 */
public class LinkHandler {
    private static final String TAG = "LinkHandler";
    private static final String WALMART_PACKAGE = "com.walmart.android";
    private static final String TARGET_PACKAGE = "com.target.ui";
    private static final String AMAZON_PACKAGE = "com.amazon.mShop.android.shopping";
    private static final String KROGER_PACKAGE = "com.kroger.mobile";
    private static final String INSTACART_PACKAGE = "com.instacart.client";
    private static final String PUBLIX_PACKAGE = "com.publix.main";
    private static final String PUBLIX_DELIVERY_PACKAGE = "com.instacart.client.publix";
    private static final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";

    /**
     * Opens a URL with an optional "Text Fragment" for automatic highlighting.
     */
    public static void openLink(Context context, String url, String title, String highlightText) {
        if (url == null || url.isEmpty()) return;

        String finalUrl = url;
        try {
            if (highlightText != null && !highlightText.isEmpty()) {
                String[] words = highlightText.split("\\s+");
                StringBuilder fragment = new StringBuilder();
                for (int i = 0; i < Math.min(words.length, 10); i++) {
                    fragment.append(words[i]).append(" ");
                }
                String encodedFragment = URLEncoder.encode(fragment.toString().trim(), StandardCharsets.UTF_8.toString());

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

    public static void openRetailerLink(Context context, String url, String retailerName) {
        if (context == null || url == null || url.trim().isEmpty()) return;

        String[] packageNames = getRetailerPackages(retailerName, url);
        for (String packageName : packageNames) {
            if (openUrlInPackage(context, url, packageName)) {
                return;
            }
        }
        for (String packageName : packageNames) {
            if (openInstalledApp(context, packageName)) {
                return;
            }
        }

        openWebViewer(context, url, retailerName != null ? retailerName : "Store");
    }

    public static void openMapLink(Context context, String url, String title) {
        if (context == null || url == null || url.trim().isEmpty()) return;
        Uri webUri = Uri.parse(url);
        Uri geoUri = buildGeoUri(webUri, title);

        if (openUriInPackage(context, geoUri, GOOGLE_MAPS_PACKAGE)) {
            return;
        }
        if (openExternalUri(context, geoUri)) {
            return;
        }
        if (openExternalUri(context, webUri)) {
            return;
        }

        openWebViewer(context, url, title != null && !title.trim().isEmpty() ? title : "Map");
    }

    private static boolean openUrlInPackage(Context context, String url, String packageName) {
        return openUriInPackage(context, Uri.parse(url), packageName);
    }

    private static boolean openUriInPackage(Context context, Uri uri, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setPackage(packageName);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "Retailer app not available for URL: " + packageName, e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unable to open retailer app: " + packageName, e);
            return false;
        }
    }

    private static boolean openExternalUri(Context context, Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No external app available for URI", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unable to open external URI", e);
            return false;
        }
    }

    private static boolean openInstalledApp(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent == null) return false;
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "Retailer app launch intent not available: " + packageName, e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unable to launch retailer app: " + packageName, e);
            return false;
        }
    }

    private static Uri buildGeoUri(Uri webUri, String title) {
        String lat = webUri.getQueryParameter("mlat");
        String lon = webUri.getQueryParameter("mlon");
        if (lat == null || lon == null) {
            lat = webUri.getQueryParameter("lat");
            lon = webUri.getQueryParameter("lon");
        }
        String label = title != null && !title.trim().isEmpty() ? title.trim() : "Store";
        String query = webUri.getQueryParameter("query");
        if (query != null && !query.trim().isEmpty()) {
            return Uri.parse("geo:0,0?q=" + Uri.encode(query.trim()));
        }
        if (lat != null && lon != null) {
            return Uri.parse("geo:" + lat + "," + lon + "?q=" + Uri.encode(lat + "," + lon + "(" + label + ")"));
        }
        return Uri.parse("geo:0,0?q=" + Uri.encode(label));
    }

    private static String[] getRetailerPackages(String retailerName, String url) {
        String combined = ((retailerName != null ? retailerName : "") + " " + (url != null ? url : "")).toLowerCase();
        if (combined.contains("walmart")) return new String[]{WALMART_PACKAGE};
        if (combined.contains("target")) return new String[]{TARGET_PACKAGE};
        if (combined.contains("amazon")) return new String[]{AMAZON_PACKAGE};
        if (combined.contains("kroger")) return new String[]{KROGER_PACKAGE};
        if (combined.contains("publix")) return new String[]{PUBLIX_PACKAGE, PUBLIX_DELIVERY_PACKAGE};
        if (combined.contains("instacart")) return new String[]{INSTACART_PACKAGE};
        return new String[0];
    }

    private static void openWebViewer(Context context, String url, String title) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("source_name", title);
        context.startActivity(intent);
    }
}

package com.ciblorenzo.whatsonmyfood;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

public class SourceViewerActivity extends BaseActivity {

    public static final String EXTRA_URL = "extra_url";
    private WebView webView;
    private ProgressBar progressBar;
    private String initialDomain;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_viewer);

        Toolbar toolbar = findViewById(R.id.source_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.source_progress_bar);
        webView = findViewById(R.id.source_web_view);

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isEmpty()) {
            finish();
            return;
        }

        initialDomain = Uri.parse(url).getHost();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String newUrl = request.getUrl().toString();
                String newDomain = request.getUrl().getHost();

                if (newDomain != null && newDomain.equals(initialDomain)) {
                    // Allow navigation within the same website
                    return false;
                } else {
                    // Prevent external navigation and prompt user
                    promptExternalBrowser(newUrl);
                    return true;
                }
            }
        });

        webView.loadUrl(url);
    }

    private void promptExternalBrowser(final String url) {
        new AlertDialog.Builder(this)
                .setTitle("External Link")
                .setMessage("This link leads to an external website. Would you like to open it in your default web browser?")
                .setPositiveButton("Open", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navigateBack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

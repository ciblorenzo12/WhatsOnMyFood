package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

public class WebViewActivity extends BaseActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private String originalHost;
    private String sourceName;
    private String searchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        Toolbar toolbar = findViewById(R.id.web_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.web_progress_bar);

        String url = getIntent().getStringExtra("url");
        sourceName = getIntent().getStringExtra("source_name");
        searchQuery = getIntent().getStringExtra("search_query");
        setToolbarTitle(displayTitle(url));

        if (url != null) {
            // Support PDF viewing via Google Docs viewer
            if (url.toLowerCase().endsWith(".pdf")) {
                url = "https://docs.google.com/viewer?embedded=true&url=" + url;
            }
            originalHost = Uri.parse(url).getHost();
            setToolbarTitle(displayTitle(url));
            setupWebView();
            webView.loadUrl(url);
        }
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        // Optimization: Enable hardware acceleration and caching
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        
        // Add a mobile user agent to prevent some sites (like Kroger) from blocking the request
        String userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36";
        webView.getSettings().setUserAgentString(userAgent);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                if (getSupportActionBar() != null) {
                    String pageTitle = view.getTitle();
                    getSupportActionBar().setTitle(pageTitle != null && !pageTitle.trim().isEmpty()
                            ? pageTitle
                            : displayTitle(url));
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (failingUrl != null && failingUrl.equals(getIntent().getStringExtra("url"))) {
                    showErrorFallback();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    showErrorFallback();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Allow all navigation within the app's webview to prevent "broken" links
                // especially when redirecting from search to actual articles
                return false;
            }
        });
    }

    private void showExternalNavigationWarning(final String url) {
        new AlertDialog.Builder(this)
                .setTitle("External Navigation")
                .setMessage("You are navigating to an external site: " + url + "\n\nHow would you like to open this link?")
                .setPositiveButton("Open in Browser", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                })
                .setNeutralButton("Open in App", (dialog, which) -> {
                    // Update original host to the new one so we don't prompt again for same site
                    originalHost = Uri.parse(url).getHost();
                    webView.loadUrl(url);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private String displayTitle(String url) {
        if (sourceName != null && !sourceName.trim().isEmpty()) {
            return sourceName.trim();
        }
        String host = url != null ? Uri.parse(url).getHost() : "";
        if (host != null && !host.trim().isEmpty()) {
            return host.replaceFirst("^www\\.", "");
        }
        return getString(R.string.source_viewer);
    }

    private void showErrorFallback() {
        String query = searchQuery != null ? searchQuery : sourceName;
        new AlertDialog.Builder(this)
                .setTitle("Link Unavailable")
                .setMessage("The direct link for '" + sourceName + "' is not responding (404/Timeout).\n\nWould you like to search for this source instead?")
                .setPositiveButton("Search on Google", (dialog, which) -> {
                    String searchUrl = "https://www.google.com/search?q=" + Uri.encode(query);
                    webView.loadUrl(searchUrl);
                })
                .setNegativeButton("Back", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_print) {
            createWebPrintJob(webView);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createWebPrintJob(WebView webView) {
        android.print.PrintManager printManager = (android.print.PrintManager) getSystemService(android.content.Context.PRINT_SERVICE);
        String jobName = getString(R.string.app_name) + " Document";
        android.print.PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
        if (printManager != null) {
            printManager.print(jobName, printAdapter, new android.print.PrintAttributes.Builder().build());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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

package com.ciblorenzo.whatsonmyfood;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.analysis.BitwiseAiCore;
import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AdditiveDatabaseActivity extends BaseActivity {
    private final List<AdditiveEntry> allEntries = new ArrayList<>();
    private AdditiveDatabaseAdapter adapter;
    private TextView resultCountTextView;
    private TextView emptyStateTextView;
    private View loadingLayout;
    private TextView loadingTextView;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private AdditiveDao additiveDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additive_database);

        Toolbar toolbar = findViewById(R.id.additive_database_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        resultCountTextView = findViewById(R.id.additive_result_count_text_view);
        emptyStateTextView = findViewById(R.id.additive_empty_state_text_view);
        loadingLayout = findViewById(R.id.additive_loading_layout);
        loadingTextView = findViewById(R.id.additive_loading_text_view);
        
        TextInputEditText searchEditText = findViewById(R.id.additive_search_edit_text);
        RecyclerView recyclerView = findViewById(R.id.additive_recycler_view);

        additiveDao = AppDatabase.getDatabase(this).additiveDao();

        adapter = new AdditiveDatabaseAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        GlassMotion.enter(findViewById(R.id.additive_intro_panel), 0L);
        GlassMotion.enter(recyclerView, 80L);
        
        loadInitialData();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                
                searchHandler.removeCallbacks(searchRunnable);
                
                filterEntries(query);
                
                if (query.length() >= 4) {
                    searchRunnable = () -> performRemoteSearch(query);
                    searchHandler.postDelayed(searchRunnable, 1500);
                } else {
                    if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadInitialData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<AdditiveEntry> fromDb = additiveDao.getAllAdditives();
            if (fromDb.isEmpty()) {
                List<AdditiveEntry> hardcoded = AdditiveDatabase.entries();
                additiveDao.insertAll(hardcoded);
                fromDb = additiveDao.getAllAdditives();
            }
            
            final List<AdditiveEntry> finalData = fromDb;
            runOnUiThread(() -> {
                allEntries.clear();
                allEntries.addAll(finalData);
                updateResults(allEntries);
            });
        });
    }

    private void performRemoteSearch(String query) {
        for (AdditiveEntry entry : allEntries) {
            if (entry.name.toLowerCase().contains(query.toLowerCase())) return;
        }

        runOnUiThread(() -> {
            if (loadingLayout != null) {
                loadingLayout.setVisibility(View.VISIBLE);
                loadingTextView.setText(getString(R.string.bitwise_searching_for, query));
            }
        });

        BitwiseAiCore.defineIngredient(this, query, LanguageManager.getLanguageName(this), new BitwiseAiCore.AiCallback() {
            @Override
            public void onResult(String jsonResult) {
                try {
                    JSONObject json = new JSONObject(jsonResult);
                    String name = json.getString("name");
                    
                    if (name.length() < 3) {
                        runOnUiThread(() -> { if (loadingLayout != null) loadingLayout.setVisibility(View.GONE); });
                        return;
                    }

                    AdditiveEntry.HealthStatus status = AdditiveEntry.HealthStatus.MODERATE;
                    String statusStr = json.optString("health_status", "MODERATE");
                    
                    if (name.toLowerCase().contains("artificial")) {
                        status = AdditiveEntry.HealthStatus.NOT_RECOMMENDED;
                    } else if (statusStr.equals("RECOMMENDED")) {
                        status = AdditiveEntry.HealthStatus.RECOMMENDED;
                    } else if (statusStr.equals("NOT_RECOMMENDED")) {
                        status = AdditiveEntry.HealthStatus.NOT_RECOMMENDED;
                    }

                    AdditiveEntry aiEntry = new AdditiveEntry(
                            name,
                            json.getString("category"),
                            "",
                            json.getString("function"),
                            json.getString("explanation"),
                            "Source: Bitwise AI Analysis",
                            json.optString("source_name", "Bitwise AI"),
                            json.optString("source_url", ""),
                            status
                    );

                    if (!aiEntry.isValid()) {
                        runOnUiThread(() -> {
                            if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
                            Toast.makeText(AdditiveDatabaseActivity.this, R.string.not_a_valid_additive, Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    // Save to database permanently
                    Executors.newSingleThreadExecutor().execute(() -> additiveDao.insertAdditive(aiEntry));

                    runOnUiThread(() -> {
                        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
                        
                        List<AdditiveEntry> current = new ArrayList<>(adapter.getEntries());
                        
                        // Prevent duplicates and partial overlaps in the results list
                        boolean alreadyPresent = false;
                        for (AdditiveEntry e : current) {
                            if (e.name.equalsIgnoreCase(aiEntry.name) || e.name.toLowerCase().contains(aiEntry.name.toLowerCase())) {
                                alreadyPresent = true; 
                                break; 
                            }
                        }
                        
                        if (!alreadyPresent) {
                            allEntries.add(0, aiEntry); // Also add to local master list
                            current.add(0, aiEntry);
                            updateResults(current);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> { if (loadingLayout != null) loadingLayout.setVisibility(View.GONE); });
                }
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> { if (loadingLayout != null) loadingLayout.setVisibility(View.GONE); });
            }
        });
    }

    private void filterEntries(String query) {
        List<AdditiveEntry> filteredEntries = new ArrayList<>();
        for (AdditiveEntry entry : allEntries) {
            if (entry.matches(query)) {
                filteredEntries.add(entry);
            }
        }
        updateResults(filteredEntries);
    }

    private void updateResults(List<AdditiveEntry> entries) {
        adapter.updateEntries(entries);
        resultCountTextView.setText(getString(R.string.additive_result_count, entries.size()));
        emptyStateTextView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

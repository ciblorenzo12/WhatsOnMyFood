package com.ciblorenzo.whatsonmyfood;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PantryActivity extends BaseActivity {

    public static final String RESULT_DATA_CHANGED = "com.ciblorenzo.whatsonmyfood.DATA_CHANGED";
    private static final String PANTRY_PREFERENCES = "pantry_preferences";
    private static final String SORT_PREFERENCE = "sort_option";

    private AppDatabase db;
    private ExecutorService executorService;
    private PantryAdapter adapter;
    private RecyclerView recyclerView;
    private List<Product> pantryProducts;
    private String currentExportType = "";
    private FirebaseUser currentUser;
    private View loadingOverlay;
    private View emptyState;
    private String uiQuery = "", uiFilter = "all";
    private PantrySortOption currentSort = PantrySortOption.RECENT;

    private final ActivityResultLauncher<Intent> detailsActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    if (result.getData().getBooleanExtra(RESULT_DATA_CHANGED, false)) {
                        loadPantryItems();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> createFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        if (currentExportType.equals("csv")) {
                            writeCsv(uri);
                        } else if (currentExportType.equals("json")) {
                            writeJson(uri);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry);

        Toolbar toolbar = findViewById(R.id.pantry_toolbar);
        setSupportActionBar(toolbar);
        GlassMotion.enter(toolbar, 0L);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();
        currentSort = PantrySortOption.fromPreference(
                getSharedPreferences(PANTRY_PREFERENCES, MODE_PRIVATE)
                        .getString(SORT_PREFERENCE, PantrySortOption.RECENT.getPreferenceValue())
        );

        recyclerView = findViewById(R.id.pantry_recycler_view);
        loadingOverlay = findViewById(R.id.loading_overlay);
        emptyState = findViewById(R.id.pantry_empty_state);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.ingredient_db_fab).setOnClickListener(v -> {
            startActivity(new Intent(PantryActivity.this, AdditiveDatabaseActivity.class));
        });
        findViewById(R.id.pantry_insights_fab).setOnClickListener(v -> {
            startActivity(new Intent(PantryActivity.this, PantryInsightsActivity.class));
        });

        ((android.widget.EditText)findViewById(R.id.ui_pantry_search)).addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){uiQuery=s.toString();applyPresentation();}
            public void afterTextChanged(android.text.Editable s){}
        });
        ((com.google.android.material.chip.ChipGroup)findViewById(R.id.ui_pantry_filters)).setOnCheckedStateChangeListener((group, ids) -> {
            int id=ids.isEmpty()?R.id.ui_filter_all:ids.get(0);
            uiFilter=id==R.id.ui_filter_review?"review":id==R.id.ui_filter_good?"good":id==R.id.ui_filter_recent?"recent":"all";
            if("recent".equals(uiFilter))selectSortOption(PantrySortOption.RECENT);else applyPresentation();
        });
        findViewById(R.id.ui_pantry_scan).setOnClickListener(v->startActivity(new Intent(this,ScanBarcodeActivity.class)));
        setupSwipeToDelete(recyclerView);
    }

    @Override protected void onResume() {
        super.onResume();
        // Scans reached through shared navigation may save products without using
        // this Activity's detail-result launcher.
        if (db != null && executorService != null && !executorService.isShutdown()) loadPantryItems();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pantry_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_sort_recent).setChecked(currentSort == PantrySortOption.RECENT);
        menu.findItem(R.id.action_sort_name).setChecked(currentSort == PantrySortOption.NAME);
        menu.findItem(R.id.action_sort_health_score).setChecked(currentSort == PantrySortOption.HEALTH_SCORE);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_export_csv) {
            currentExportType = "csv";
            createFile("pantry.csv", "text/csv");
            return true;
        } else if (item.getItemId() == R.id.action_pantry_insights) {
            startActivity(new Intent(this, PantryInsightsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_export_json) {
            currentExportType = "json";
            createFile("pantry.json", "application/json");
            return true;
        } else if (item.getItemId() == R.id.action_sort_recent) {
            selectSortOption(PantrySortOption.RECENT);
            return true;
        } else if (item.getItemId() == R.id.action_sort_name) {
            selectSortOption(PantrySortOption.NAME);
            return true;
        } else if (item.getItemId() == R.id.action_sort_health_score) {
            selectSortOption(PantrySortOption.HEALTH_SCORE);
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createFile(String defaultFileName, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);
        createFileLauncher.launch(intent);
    }

    private void selectSortOption(PantrySortOption option) {
        if (option == null) return;
        currentSort = option;
        getSharedPreferences(PANTRY_PREFERENCES, MODE_PRIVATE)
                .edit()
                .putString(SORT_PREFERENCE, option.getPreferenceValue())
                .apply();
        invalidateOptionsMenu();
        loadPantryItems();
    }

    private void loadPantryItems() {
        if (currentUser == null) return;
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
            pantryProducts = getSortedPantryProducts(currentUser.getUid());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                PantryListStateViewBinder.bind(
                        recyclerView,
                        emptyState,
                        pantryProducts == null ? 0 : pantryProducts.size()
                );
                if (adapter == null) {
                    adapter = new PantryAdapter(pantryProducts, product -> {
                        detailsActivityLauncher.launch(
                                PantryNavigation.productDetailsIntent(PantryActivity.this, product)
                        );
                    }, (product, score) -> executorService.execute(() ->
                            db.productDao().updateUserIngredientRiskScore(product.barcode, score)
                    ));
                    recyclerView.setAdapter(adapter);
                    GlassMotion.enter(recyclerView, 80L);
                } else {
                    adapter.updateList(new java.util.ArrayList<>(pantryProducts));
                }
                applyPresentation();
            });
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    if(isFinishing()||isDestroyed())return;
                    loadingOverlay.setVisibility(View.GONE);
                    com.ciblorenzo.whatsonmyfood.ui.ContentStateView state=findViewById(R.id.ui_pantry_error);
                    state.show(R.string.ui_error,false);state.action(R.string.ui_retry,v->loadPantryItems());
                });
            }
        });
    }

    private void applyPresentation() {
        if(adapter==null||pantryProducts==null)return;
        java.util.List<Product> visible=com.ciblorenzo.whatsonmyfood.ui.PantryPresentation.filter(pantryProducts,uiQuery,uiFilter);
        adapter.updateList(visible);
        PantryListStateViewBinder.bind(recyclerView,emptyState,visible.size());
        ((android.widget.TextView)findViewById(R.id.ui_pantry_empty_text)).setText(pantryProducts.isEmpty()?R.string.pantry_empty_message:R.string.ui_no_results);
        String summary=getString(R.string.ui_pantry_summary,pantryProducts.size(),com.ciblorenzo.whatsonmyfood.ui.PantryPresentation.reviewCount(pantryProducts));
        ((android.widget.TextView)findViewById(R.id.ui_pantry_summary)).setText(summary);
        ((android.widget.TextView)findViewById(R.id.ui_pantry_insights_summary)).setText(summary+"\n\n"+getString(R.string.ui_pantry_insights_body));
        findViewById(R.id.ui_pantry_error).setVisibility(View.GONE);
    }

    private List<Product> getSortedPantryProducts(String userId) {
        switch (currentSort) {
            case NAME:
                return db.productDao().getPantryProductsByName(userId);
            case HEALTH_SCORE:
                return db.productDao().getPantryProductsByHealthScore(userId);
            case RECENT:
            default:
                return db.productDao().getPantryProducts(userId);
        }
    }

    private void setupSwipeToDelete(RecyclerView recyclerView) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (currentUser == null) return;
                int position = viewHolder.getAdapterPosition();
                if(position==RecyclerView.NO_POSITION || position>=adapter.getItemCount())return;
                Product product = adapter.getProductAt(position);

                executorService.execute(() -> {
                    db.productDao().deletePantryProduct(product.barcode, currentUser.getUid());
                    runOnUiThread(() -> loadPantryItems());
                });
            }

        }).attachToRecyclerView(recyclerView);
    }

    private void writeCsv(Uri uri) {
        executorService.execute(() -> {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri);
                 OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {

                writer.append("\"Barcode\",\"Name\",\"Brand\",\"Quantity\"\n");

                for (Product product : pantryProducts) {
                    writer.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            product.barcode, 
                            escapeCsv(product.productName),
                            escapeCsv(product.brands),
                            escapeCsv(product.quantity)));
                }
                runOnUiThread(() -> Toast.makeText(this, "CSV export successful", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "CSV export failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void writeJson(Uri uri) {
        executorService.execute(() -> {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri);
                 OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                JSONArray jsonArray = new JSONArray();
                for (Product product : pantryProducts) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("barcode", product.barcode);
                    jsonObject.put("product_name", product.productName);
                    jsonObject.put("brands", product.brands);
                    jsonObject.put("quantity", product.quantity);
                    jsonArray.put(jsonObject);
                }
                writer.write(jsonArray.toString(4));
                runOnUiThread(() -> Toast.makeText(this, "JSON export successful", Toast.LENGTH_SHORT).show());
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "JSON export failed", Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

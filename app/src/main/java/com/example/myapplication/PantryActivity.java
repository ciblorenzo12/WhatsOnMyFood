package com.example.myapplication;

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

import com.example.myapplication.utils.GlassMotion;
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

    public static final String RESULT_DATA_CHANGED = "com.example.myapplication.DATA_CHANGED";

    private AppDatabase db;
    private ExecutorService executorService;
    private PantryAdapter adapter;
    private RecyclerView recyclerView;
    private List<Product> pantryProducts;
    private String currentExportType = "";
    private FirebaseUser currentUser;
    private View loadingOverlay;

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

        recyclerView = findViewById(R.id.pantry_recycler_view);
        loadingOverlay = findViewById(R.id.loading_overlay);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.ingredient_db_fab).setOnClickListener(v -> {
            startActivity(new Intent(PantryActivity.this, AdditiveDatabaseActivity.class));
        });

        loadPantryItems();
        setupSwipeToDelete(recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pantry_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_export_csv) {
            currentExportType = "csv";
            createFile("pantry.csv", "text/csv");
            return true;
        } else if (item.getItemId() == R.id.action_export_json) {
            currentExportType = "json";
            createFile("pantry.json", "application/json");
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

    private void loadPantryItems() {
        if (currentUser == null) return;
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            pantryProducts = db.productDao().getPantryProducts(currentUser.getUid());
            runOnUiThread(() -> {
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                if (adapter == null) {
                    adapter = new PantryAdapter(pantryProducts, product -> {
                        Intent intent = new Intent(PantryActivity.this, ProductDetailsActivity.class);
                        intent.putExtra(ProductDetailsActivity.EXTRA_BARCODE, product.barcode);
                        detailsActivityLauncher.launch(intent);
                    });
                    recyclerView.setAdapter(adapter);
                    GlassMotion.enter(recyclerView, 80L);
                } else {
                    adapter.updateList(pantryProducts);
                }
            });
        });
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

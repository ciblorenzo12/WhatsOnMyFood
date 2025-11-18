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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PantryActivity extends AppCompatActivity {

    public static final String RESULT_DATA_CHANGED = "com.example.myapplication.DATA_CHANGED";

    private AppDatabase db;
    private ExecutorService executorService;
    private PantryAdapter adapter;
    private RecyclerView recyclerView;
    private List<Product> pantryProducts;
    private String currentExportType = "";

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
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();

        recyclerView = findViewById(R.id.pantry_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
        executorService.execute(() -> {
            pantryProducts = db.productDao().getPantryProducts();
            runOnUiThread(() -> {
                adapter = new PantryAdapter(pantryProducts, product -> {
                    Intent intent = new Intent(PantryActivity.this, ProductDetailsActivity.class);
                    intent.putExtra(ProductDetailsActivity.EXTRA_BARCODE, product.barcode);
                    detailsActivityLauncher.launch(intent);
                });
                recyclerView.setAdapter(adapter);
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
                int position = viewHolder.getAdapterPosition();
                Product product = adapter.getProductAt(position);

                executorService.execute(() -> {
                    db.productDao().deletePantryProduct(product.barcode);
                    runOnUiThread(() -> loadPantryItems());
                });
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    PantryAdapter.PantryViewHolder holder = (PantryAdapter.PantryViewHolder) viewHolder;
                    getDefaultUIUtil().onDraw(c, recyclerView, holder.cardView, dX, dY, actionState, isCurrentlyActive);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void writeCsv(Uri uri) {
        executorService.execute(() -> {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri);
                 OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {

                // Header
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
                writer.write(jsonArray.toString(4)); // Pretty print
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

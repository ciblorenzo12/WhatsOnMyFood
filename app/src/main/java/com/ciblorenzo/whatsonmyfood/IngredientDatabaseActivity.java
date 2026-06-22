package com.ciblorenzo.whatsonmyfood;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class IngredientDatabaseActivity extends BaseActivity {

    private List<IngredientInfo> allIngredients = new ArrayList<>();
    private IngredientInfoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_database);

        Toolbar toolbar = findViewById(R.id.db_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ingredient Database");
        }

        loadData();

        RecyclerView recyclerView = findViewById(R.id.db_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IngredientInfoAdapter(allIngredients);
        recyclerView.setAdapter(adapter);

        EditText searchEdit = findViewById(R.id.db_search_edit);
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadData() {
        try {
            InputStream is = getAssets().open("ingredient_db.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                allIngredients.add(new IngredientInfo(
                    obj.getString("name"),
                    obj.getString("purpose"),
                    obj.getString("health_note"),
                    obj.getString("category")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filter(String query) {
        List<IngredientInfo> filtered = new ArrayList<>();
        for (IngredientInfo info : allIngredients) {
            if (info.getName().toLowerCase().contains(query.toLowerCase()) ||
                info.getCategory().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(info);
            }
        }
        adapter.updateList(filtered);
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigateBack();
        return true;
    }
}

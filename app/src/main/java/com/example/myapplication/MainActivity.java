package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.myapplication.retail.RetailerAlternative;
import com.example.myapplication.retail.RetailerAlternativeAdapter;
import com.example.myapplication.utils.GlassMotion;
import com.example.myapplication.utils.LocationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends BaseActivity {

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.get(android.Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseLocationGranted = result.get(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                }
            });

    private static class HealthTip {
        final String tip;
        final String source;
        HealthTip(String tip, String source) {
            this.tip = tip;
            this.source = source;
        }
    }

    private final List<HealthTip> dailyTips = Arrays.asList(
            new HealthTip("Try to avoid products with more than 5 ingredients you can't easily pronounce.", "Common Health Wisdom"),
            new HealthTip("Whole foods are always safer than highly processed alternatives. Look for single-ingredient items.", "WHO Healthy Diet Guidelines"),
            new HealthTip("Added sugars are often hidden under names like 'maltodextrin' or 'high fructose corn syrup'.", "FDA Consumer Info"),
            new HealthTip("Sodium levels in canned goods can be 20x higher than fresh versions. Always rinse before eating.", "American Heart Association"),
            new HealthTip("Natural flavors aren't always natural. They are often complex chemical mixtures.", "Environmental Working Group"),
            new HealthTip("Ultra-processed foods are linked to a higher risk of metabolic syndrome. Shop the perimeter of the store.", "Nutritional Science"),
            new HealthTip("Artificial dyes like Red 40 can affect focus in children. Switch to products with beet or turmeric coloring.", "Pediatric Health Studies"),
            new HealthTip("Seed oils high in Omega-6 can promote inflammation. Try swapping for Avocado or Olive oil.", "Wellness Research"),
            new HealthTip("Drinking a glass of water before every meal can naturally regulate appetite and digestion.", "Hydration Science"),
            new HealthTip("Reading the fiber-to-carb ratio can help you find grains that keep your energy stable all day.", "Metabolic Health")
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        requestLocationPermission();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Bottom handled by Nav
            return insets;
        });

        setupEntranceAnimations();
        setupDailyTip();
        setupQuickActions();
        setupRecommendations();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home); 
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    return true;
                } else if (itemId == R.id.navigation_scan) {
                    startActivity(new Intent(MainActivity.this, ScanBarcodeActivity.class));
                    return true;
                } else if (itemId == R.id.navigation_pantry) {
                    startActivity(new Intent(MainActivity.this, PantryActivity.class));
                    return true;
                } else if (itemId == R.id.navigation_database) {
                    startActivity(new Intent(MainActivity.this, AdditiveDatabaseActivity.class));
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    return true;
                }
                return false;
            }
        });
    }

    private void setupEntranceAnimations() {
        GlassMotion.enter(findViewById(R.id.home_title_text_view), 0L);
        GlassMotion.enter(findViewById(R.id.daily_tip_card), 150L);
        GlassMotion.enter(findViewById(R.id.action_scan), 300L);
        GlassMotion.enter(findViewById(R.id.action_pantry), 350L);
        GlassMotion.enter(findViewById(R.id.action_database), 400L);
        GlassMotion.enter(findViewById(R.id.action_marketplace), 450L);
        GlassMotion.enter(findViewById(R.id.recommendations_recycler_view), 600L);
    }

    private void setupDailyTip() {
        HealthTip tip = dailyTips.get(new Random().nextInt(dailyTips.size()));
        TextView tipTextView = findViewById(R.id.daily_tip_text);
        TextView sourceTextView = findViewById(R.id.daily_tip_source);
        
        if (tipTextView != null) tipTextView.setText(tip.tip);
        if (sourceTextView != null) {
            sourceTextView.setText(getString(R.string.source_prefix, tip.source));
        }
    }

    private void setupQuickActions() {
        findViewById(R.id.action_scan).setOnClickListener(v -> startActivity(new Intent(this, ScanBarcodeActivity.class)));
        findViewById(R.id.action_pantry).setOnClickListener(v -> startActivity(new Intent(this, PantryActivity.class)));
        findViewById(R.id.action_database).setOnClickListener(v -> startActivity(new Intent(this, AdditiveDatabaseActivity.class)));
        findViewById(R.id.action_marketplace).setOnClickListener(v -> {
            Toast.makeText(this, "Scan a product to see marketplace comparisons!", Toast.LENGTH_SHORT).show();
        });

        GlassMotion.attachPress(findViewById(R.id.action_scan));
        GlassMotion.attachPress(findViewById(R.id.action_pantry));
        GlassMotion.attachPress(findViewById(R.id.action_database));
        GlassMotion.attachPress(findViewById(R.id.action_marketplace));
    }

    private void setupRecommendations() {
        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.recommendations_recycler_view);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        
        RetailerAlternativeAdapter adapter = new RetailerAlternativeAdapter(this);
        rv.setAdapter(adapter);

        // Featured recommendations (Higher variety)
        List<RetailerAlternative> featured = new ArrayList<>();
        featured.add(new RetailerAlternative("Vintage Cola", "Olipop", "High fiber alternative with 2g sugar.", "92/100", "Available at Target, Kroger", "https://www.google.com/search?q=olipop+vintage+cola", ""));
        featured.add(new RetailerAlternative("Ancient Grain Granola", "Purely Elizabeth", "Low sugar cereal swap with chia seeds.", "95/100", "Available at Whole Foods, Sprouts", "https://www.google.com/search?q=purely+elizabeth+granola", ""));
        featured.add(new RetailerAlternative("Cassava Flour Chips", "Siete", "Grain-free snack made with avocado oil.", "88/100", "Available at Walmart, Costco", "https://www.google.com/search?q=siete+chips", ""));
        featured.add(new RetailerAlternative("Organic Skyr", "Siggi's", "High protein, low sugar yogurt option.", "94/100", "Available at Publix, Safeway", "https://www.google.com/search?q=siggi+yogurt", ""));
        
        adapter.submitList(featured);

        View viewAll = findViewById(R.id.view_all_swaps);
        if (viewAll != null) {
            viewAll.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, AdditiveDatabaseActivity.class));
                Toast.makeText(this, "Explore the database for more healthy insights!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void requestLocationPermission() {
        if (!LocationHelper.hasPermissions(this)) {
            locationPermissionRequest.launch(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }
}

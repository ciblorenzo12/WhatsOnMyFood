package com.ciblorenzo.whatsonmyfood;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PantryArInsightsActivity extends BaseActivity {
    private PreviewView previewView;
    private PantryArGraphOverlayView overlayView;
    private AppDatabase db;
    private ExecutorService executorService;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private FirebaseUser currentUser;

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_ar, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry_ar_insights);

        Toolbar toolbar = findViewById(R.id.pantry_ar_toolbar);
        setSupportActionBar(toolbar);
        GlassMotion.enter(toolbar, 0L);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        previewView = findViewById(R.id.ar_preview_view);
        overlayView = findViewById(R.id.ar_graph_overlay);
        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        loadRiskData();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void loadRiskData() {
        if (currentUser == null) return;
        executorService.execute(() -> {
            List<Product> products = db.productDao().getPantryProducts(currentUser.getUid());
            List<PantryRiskScorer.RiskItem> items = PantryRiskScorer.scoreProducts(products);
            runOnUiThread(() -> overlayView.setItems(items));
        });
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview);
            } catch (Exception e) {
                Toast.makeText(this, "Could not start AR camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

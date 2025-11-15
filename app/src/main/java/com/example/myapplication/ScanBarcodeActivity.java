package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanBarcodeActivity extends AppCompatActivity {

    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private TextView offlineIndicator;
    private BarcodeGraphic barcodeGraphic;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private boolean isLaunchingDetails = false;
    private boolean isScanLocked = false;

    private final Handler launchHandler = new Handler(Looper.getMainLooper());
    private Runnable launchRunnable;

    // Modern photo picker launcher
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                decodeBarcodeFromBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    });

    // Launcher for requesting storage permission
    private final ActivityResultLauncher<String> requestStoragePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Toast.makeText(this, "Permission granted. Please tap the import button again.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Permission denied. You can't import images without it.", Toast.LENGTH_LONG).show();
        }
    });

    // Launcher for requesting camera permission
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required to use the scanner.", Toast.LENGTH_LONG).show();
            finish(); // Close the app if camera permission is denied
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        previewView = findViewById(R.id.preview_view);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        offlineIndicator = findViewById(R.id.offline_indicator);
        cameraExecutor = Executors.newSingleThreadExecutor();

        barcodeGraphic = new BarcodeGraphic(graphicOverlay);
        graphicOverlay.add(barcodeGraphic);

        ImageButton importButton = findViewById(R.id.import_button);
        importButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                pickMediaLauncher.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint({"UnsafeOptInUsageError", "ClickableViewAccessibility"})
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(1920, 1080))
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1920, 1080))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (image.getImage() == null || isLaunchingDetails) {
                image.close();
                return;
            }

            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            graphicOverlay.setImageSourceInfo(
                    rotationDegrees == 90 || rotationDegrees == 270 ? image.getHeight() : image.getWidth(),
                    rotationDegrees == 90 || rotationDegrees == 270 ? image.getWidth() : image.getHeight(),
                    false);

            InputImage inputImage = InputImage.fromMediaImage(image.getImage(), rotationDegrees);
            scanner.process(inputImage).addOnSuccessListener(barcodes -> {
                graphicOverlay.setDrawStaticFrame(barcodes.isEmpty());
                if (!barcodes.isEmpty()) {
                    Barcode barcode = barcodes.get(0);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        barcodeGraphic.update(barcode);
                    }, 100);

                    Rect boundingBox = barcode.getBoundingBox();
                    if (boundingBox != null) {
                        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(previewView.getWidth(), previewView.getHeight());
                        FocusMeteringAction action = new FocusMeteringAction.Builder(factory.createPoint(boundingBox.centerX(), boundingBox.centerY()), FocusMeteringAction.FLAG_AF).build();
                        camera.getCameraControl().startFocusAndMetering(action);
                    }

                    if (!isScanLocked) {
                        isScanLocked = true;
                        launchRunnable = () -> handleBarcode(barcode.getRawValue());
                        launchHandler.postDelayed(launchRunnable, 300);
                    }

                } else {
                    barcodeGraphic.hide();
                    if (isScanLocked) {
                        launchHandler.removeCallbacks(launchRunnable);
                        isScanLocked = false;
                    }
                }
            }).addOnCompleteListener(task -> image.close());
        });

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            setupTorchControl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTorchControl() {
        ToggleButton torchButton = findViewById(R.id.torch_button);
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            torchButton.setVisibility(View.VISIBLE);
            camera.getCameraInfo().getTorchState().observe(this, torchState -> {
                if (torchState != null) {
                    torchButton.setChecked(torchState == TorchState.ON);
                }
            });
            torchButton.setOnClickListener(v -> camera.getCameraControl().enableTorch(torchButton.isChecked()));
        } else {
            torchButton.setVisibility(View.GONE);
        }
    }

    private void decodeBarcodeFromBitmap(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        BarcodeScanning.getClient().process(image).addOnSuccessListener(barcodes -> {
            if (!barcodes.isEmpty()) {
                String rawValue = barcodes.get(0).getRawValue();
                if (rawValue != null) {
                    handleBarcode(rawValue);
                }
            } else {
                Toast.makeText(this, "No barcode found in image", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error scanning image", Toast.LENGTH_SHORT).show());
    }

    private void handleBarcode(String barcode) {
        if (isFinishing() || isLaunchingDetails) {
            return;
        }
        isLaunchingDetails = true;

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        graphicOverlay.setVisibility(View.GONE);

        ProductDetailsFragment fragment = ProductDetailsFragment.newInstance(barcode);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentViewDestroyed(fm, f);
                if (f == fragment) {
                    graphicOverlay.setVisibility(View.VISIBLE);
                    resetScanner();
                    if (ContextCompat.checkSelfPermission(ScanBarcodeActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startCamera();
                    }
                    getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(this);
                }
            }
        }, false);
        fragment.show(getSupportFragmentManager(), fragment.getTag());
    }

    private void resetScanner() {
        isLaunchingDetails = false;
        isScanLocked = false;
        launchHandler.removeCallbacks(launchRunnable);
        barcodeGraphic.hide();
        graphicOverlay.setDrawStaticFrame(true);
    }

    private void checkOfflineStatus(){
        if (offlineIndicator != null) {
            offlineIndicator.setVisibility(NetworkUtils.isOnline(this) ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetScanner();
        checkOfflineStatus();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}

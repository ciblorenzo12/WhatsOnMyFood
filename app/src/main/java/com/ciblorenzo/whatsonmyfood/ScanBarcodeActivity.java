package com.ciblorenzo.whatsonmyfood;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
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
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanBarcodeActivity extends BaseActivity {

    private static final String SCANNER_PREFERENCES = "scanner_preferences";
    private static final String CAMERA_PERMISSION_REQUESTED = "camera_permission_requested";
    private static final long BARCODE_SCAN_TIMEOUT_MS = 12_000L;

    public enum ScanMode {
        BARCODE, INGREDIENTS
    }

    private PreviewView previewView;
    private TextView offlineIndicator, modeTextView, statusTextView, modeToggleHintText;
    private LinearLayout scannerStatePanel;
    private TextView scannerStateTitle, scannerStateMessage;
    private Button scannerStatePrimaryButton, scannerStateSecondaryButton;
    private ImageButton modeToggleButton;
    private ToggleButton barcodeAiToggleButton, torchButton;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private boolean isScanLocked = false;
    private boolean barcodeAiEnabled = true;
    private ScanMode currentMode = ScanMode.BARCODE;
    private TextRecognizer textRecognizer;
    private BarcodeScanner barcodeScanner;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private Boolean lastKnownOnline;
    private boolean networkCallbackRegistered;
    private boolean cameraStarting;
    private boolean returningFromAppSettings;
    private ScanMode pendingRecoveryMode;

    private final Handler launchHandler = new Handler(Looper.getMainLooper());
    private Runnable launchRunnable;
    private final Runnable barcodeScanTimeoutRunnable = this::showFailedScanRecovery;
    private AnimatorSet modeToggleGlowAnimator;
    private final Runnable hideConnectionMessageRunnable = () -> {
        if (offlineIndicator != null && NetworkUtils.isOnline(this)) {
            offlineIndicator.setVisibility(View.GONE);
        }
    };

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                if (currentMode == ScanMode.INGREDIENTS) {
                    decodeBarcodeFromBitmap(bitmap);
                } else {
                    decodeBarcodeFromBitmap(bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            hideScannerState();
            startCamera();
        } else {
            showCameraPermissionDenied();
        }
    });

    private ScanningOverlayView scanningOverlay;
    private ProgressBar transitionProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        previewView = findViewById(R.id.preview_view);
        scanningOverlay = findViewById(R.id.scanning_overlay);
        offlineIndicator = findViewById(R.id.offline_indicator);
        modeTextView = findViewById(R.id.mode_text_view);
        statusTextView = findViewById(R.id.status_text_view);
        modeToggleButton = findViewById(R.id.mode_toggle_button);
        modeToggleHintText = findViewById(R.id.mode_toggle_hint_text);
        barcodeAiToggleButton = findViewById(R.id.barcode_ai_toggle_button);
        torchButton = findViewById(R.id.torch_button);
        transitionProgressBar = findViewById(R.id.loading_progress_bar);
        scannerStatePanel = findViewById(R.id.scanner_state_panel);
        scannerStateTitle = findViewById(R.id.scanner_state_title);
        scannerStateMessage = findViewById(R.id.scanner_state_message);
        scannerStatePrimaryButton = findViewById(R.id.scanner_state_primary_button);
        scannerStateSecondaryButton = findViewById(R.id.scanner_state_secondary_button);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        cameraExecutor = Executors.newSingleThreadExecutor();

        getSupportFragmentManager().setFragmentResultListener(
                ProductDetailsFragment.SCAN_RECOVERY_RESULT,
                this,
                (requestKey, result) -> {
                    String action = result.getString(ProductDetailsFragment.SCAN_RECOVERY_ACTION, "");
                    pendingRecoveryMode = ProductDetailsFragment.SCAN_RECOVERY_INGREDIENTS.equals(action)
                            ? ScanMode.INGREDIENTS
                            : ScanMode.BARCODE;
                }
        );

        if (scanningOverlay != null) {
            scanningOverlay.setMode(ScanningOverlayView.OverlayMode.BARCODE);
            scanningOverlay.stopScanning();
        }

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        modeToggleButton.setOnClickListener(v -> {
            hideModeToggleHint();
            toggleScanMode();
        });
        if (barcodeAiToggleButton != null) {
            barcodeAiToggleButton.setChecked(barcodeAiEnabled);
            barcodeAiToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> barcodeAiEnabled = isChecked);
        }

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> navigateBack());

        ImageButton importButton = findViewById(R.id.import_button);
        importButton.setOnClickListener(v -> {
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        prepareCameraAccess();

        launchHandler.postDelayed(this::showModeToggleHintIfNeeded, 900);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerNetworkCallback();
        updateConnectionStatus();
        if (returningFromAppSettings) {
            returningFromAppSettings = false;
            if (hasCameraPermission()) {
                hideScannerState();
                startCamera();
            } else {
                showCameraPermissionDenied();
            }
        } else if (hasCameraPermission() && cameraProvider != null && !isScannerStateVisible()) {
            scheduleBarcodeScanTimeout();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterNetworkCallback();
        launchHandler.removeCallbacks(hideConnectionMessageRunnable);
        cancelBarcodeScanTimeout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        launchHandler.removeCallbacksAndMessages(null);
        stopModeToggleGlow();
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void prepareCameraAccess() {
        if (hasCameraPermission()) {
            startCamera();
            return;
        }

        SharedPreferences preferences = getSharedPreferences(SCANNER_PREFERENCES, MODE_PRIVATE);
        if (!preferences.getBoolean(CAMERA_PERMISSION_REQUESTED, false)) {
            showScannerState(
                    R.string.camera_permission_first_title,
                    R.string.camera_permission_first_message,
                    R.string.continue_to_camera,
                    () -> {
                        preferences.edit().putBoolean(CAMERA_PERMISSION_REQUESTED, true).apply();
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                    },
                    R.string.not_now,
                    this::showCameraPermissionDenied
            );
        } else {
            showCameraPermissionDenied();
        }
    }

    private void showCameraPermissionDenied() {
        if (statusTextView != null) statusTextView.setText(R.string.camera_permission_denied_title);
        showScannerState(
                R.string.camera_permission_denied_title,
                R.string.camera_permission_denied_message,
                R.string.open_android_settings,
                this::openAndroidAppSettings,
                R.string.scan_from_photo,
                this::openPhotoPicker
        );
    }

    private void openAndroidAppSettings() {
        returningFromAppSettings = true;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void openPhotoPicker() {
        pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void showScannerState(int titleRes, int messageRes, int primaryTextRes, Runnable primaryAction,
                                  int secondaryTextRes, Runnable secondaryAction) {
        cancelBarcodeScanTimeout();
        isScanLocked = true;
        if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.GONE);
        if (scanningOverlay != null) scanningOverlay.stopScanning();
        scannerStateTitle.setText(titleRes);
        scannerStateMessage.setText(messageRes);
        scannerStatePrimaryButton.setText(primaryTextRes);
        scannerStateSecondaryButton.setText(secondaryTextRes);
        scannerStatePrimaryButton.setOnClickListener(v -> primaryAction.run());
        scannerStateSecondaryButton.setOnClickListener(v -> secondaryAction.run());
        if (modeToggleButton != null) modeToggleButton.setEnabled(false);
        if (barcodeAiToggleButton != null) barcodeAiToggleButton.setEnabled(false);
        if (torchButton != null) torchButton.setEnabled(false);
        scannerStatePanel.setVisibility(View.VISIBLE);
    }

    private void hideScannerState() {
        if (scannerStatePanel != null) scannerStatePanel.setVisibility(View.GONE);
        if (scannerStatePrimaryButton != null) scannerStatePrimaryButton.setOnClickListener(null);
        if (scannerStateSecondaryButton != null) scannerStateSecondaryButton.setOnClickListener(null);
        if (modeToggleButton != null) modeToggleButton.setEnabled(true);
        if (barcodeAiToggleButton != null) barcodeAiToggleButton.setEnabled(true);
        if (torchButton != null) torchButton.setEnabled(true);
    }

    private boolean isScannerStateVisible() {
        return scannerStatePanel != null && scannerStatePanel.getVisibility() == View.VISIBLE;
    }

    private void startCamera() {
        if (!hasCameraPermission()) {
            showCameraPermissionDenied();
            return;
        }
        if (cameraProvider != null) {
            hideScannerState();
            bindCameraUseCases(cameraProvider);
            return;
        }
        if (cameraStarting) return;
        cameraStarting = true;
        hideScannerState();
        isScanLocked = true;
        if (statusTextView != null) statusTextView.setText(R.string.camera_starting);
        if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.VISIBLE);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                showCameraUnavailable();
            } finally {
                cameraStarting = false;
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void showCameraUnavailable() {
        if (statusTextView != null) statusTextView.setText(R.string.camera_unavailable_title);
        showScannerState(
                R.string.camera_unavailable_title,
                R.string.camera_unavailable_message,
                R.string.try_again,
                this::startCamera,
                R.string.open_android_settings,
                this::openAndroidAppSettings
        );
    }

    private void toggleScanMode() {
        cancelBarcodeScanTimeout();
        hideScannerState();
        isScanLocked = false;
        if (currentMode == ScanMode.BARCODE) {
            currentMode = ScanMode.INGREDIENTS;
            modeTextView.setText(R.string.ingredient_mode);
            if (statusTextView != null) statusTextView.setText(R.string.point_camera_ingredients);
            modeToggleButton.setImageResource(R.drawable.ic_ingredients_list);
            modeToggleButton.setContentDescription(getString(R.string.ingredient_mode));
            if (barcodeAiToggleButton != null) barcodeAiToggleButton.setVisibility(View.GONE);
            runOnUiThread(() -> {
                if (scanningOverlay != null) {
                    scanningOverlay.setMode(ScanningOverlayView.OverlayMode.INGREDIENTS);
                    scanningOverlay.startScanning();
                }
            });
        } else {
            currentMode = ScanMode.BARCODE;
            modeTextView.setText(R.string.barcode_mode);
            if (statusTextView != null) statusTextView.setText(R.string.point_camera_barcode);
            modeToggleButton.setImageResource(R.drawable.ic_scan);
            modeToggleButton.setContentDescription(getString(R.string.barcode_mode));
            if (barcodeAiToggleButton != null) barcodeAiToggleButton.setVisibility(View.VISIBLE);
            runOnUiThread(() -> {
                if (scanningOverlay != null) {
                    scanningOverlay.setMode(ScanningOverlayView.OverlayMode.BARCODE);
                    scanningOverlay.startScanning();
                }
            });
        }
        
        if (cameraProvider != null) {
            bindCameraUseCases(cameraProvider);
        }
    }

    private void showModeToggleHintIfNeeded() {
        if (currentMode != ScanMode.BARCODE || modeToggleButton == null || modeToggleHintText == null) {
            return;
        }

        modeToggleHintText.setAlpha(0f);
        modeToggleHintText.setTranslationY(12f);
        modeToggleHintText.setVisibility(View.VISIBLE);
        modeToggleHintText.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220L)
                .start();
        startModeToggleGlow();

        launchHandler.postDelayed(() -> {
            if (currentMode == ScanMode.BARCODE) {
                hideModeToggleHint();
            }
        }, 6500L);
    }

    private void startModeToggleGlow() {
        stopModeToggleGlow();
        if (modeToggleButton == null) return;
        modeToggleButton.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(modeToggleButton, View.SCALE_X, 1f, 1.08f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(modeToggleButton, View.SCALE_Y, 1f, 1.08f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(modeToggleButton, View.ALPHA, 1f, 0.92f, 1f);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        modeToggleGlowAnimator = new AnimatorSet();
        modeToggleGlowAnimator.playTogether(scaleX, scaleY, alpha);
        modeToggleGlowAnimator.setDuration(900L);
        modeToggleGlowAnimator.start();
    }

    private void stopModeToggleGlow() {
        if (modeToggleGlowAnimator != null) {
            modeToggleGlowAnimator.cancel();
            modeToggleGlowAnimator = null;
        }
        if (modeToggleButton != null) {
            modeToggleButton.animate().cancel();
            modeToggleButton.setScaleX(1f);
            modeToggleButton.setScaleY(1f);
            modeToggleButton.setAlpha(1f);
            modeToggleButton.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }

    private void hideModeToggleHint() {
        if (modeToggleHintText == null || modeToggleHintText.getVisibility() != View.VISIBLE) {
            stopModeToggleGlow();
            return;
        }
        stopModeToggleGlow();
        modeToggleHintText.animate()
                .alpha(0f)
                .translationY(8f)
                .setDuration(180L)
                .withEndAction(() -> modeToggleHintText.setVisibility(View.GONE))
                .start();
    }

    @SuppressLint({"UnsafeOptInUsageError", "ClickableViewAccessibility"})
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(1280, 720))
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (isScanLocked || image.getImage() == null) {
                image.close();
                return;
            }

            InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());
            runOnUiThread(() -> {
                if (scanningOverlay != null) {
                    scanningOverlay.setImageSourceInfo(inputImage.getWidth(), inputImage.getHeight(), false);
                }
            });

            if (currentMode == ScanMode.BARCODE) {
                barcodeScanner.process(inputImage).addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        Barcode barcode = barcodes.get(0);
                        String barcodeValue = barcode.getRawValue();
                        Rect barcodeBox = barcode.getBoundingBox();
                        runOnUiThread(() -> {
                            if (scanningOverlay != null) {
                                scanningOverlay.updateBarcode(barcodeBox, barcodeValue != null);
                            }
                        });

                        if (!isScanLocked && barcodeValue != null) {
                            cancelBarcodeScanTimeout();
                            isScanLocked = true;
                            launchRunnable = () -> handleBarcode(barcodeValue);
                            launchHandler.postDelayed(launchRunnable, 300);
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (scanningOverlay != null) {
                                scanningOverlay.updateBarcode(null, false);
                            }
                        });
                    }
                }).addOnCompleteListener(task -> image.close());
            } else {
                textRecognizer.process(inputImage).addOnSuccessListener(text -> {
                    int bestIngredientConfidence = 0;
                    int bestIndex = -1;
                    List<Rect> textBoxes = new ArrayList<>();
                    
                    for (com.google.mlkit.vision.text.Text.TextBlock block : text.getTextBlocks()) {
                        String blockText = block.getText();
                        int ingredientConfidence = getIngredientConfidence(blockText);
                        
                        Rect blockBox = block.getBoundingBox();
                        int mappedIndex = -1;
                        if (blockBox != null) {
                            mappedIndex = textBoxes.size();
                            textBoxes.add(blockBox);
                        }
                        
                        if (ingredientConfidence > bestIngredientConfidence) {
                            bestIngredientConfidence = ingredientConfidence;
                            bestIndex = mappedIndex;
                        }
                    }

                    final int mappedBestIndex = bestIndex;
                    runOnUiThread(() -> {
                        if (scanningOverlay != null) {
                            scanningOverlay.updateTextBlocks(textBoxes, mappedBestIndex);
                        }
                    });

                    // Ingredient Mode should launch from an actual ingredient label, not front-package branding.
                    if (bestIngredientConfidence > 25) {
                        if (!isScanLocked) {
                            isScanLocked = true;
                            final String fullContextText = text.getText();
                            Bitmap rawBitmap = BitmapUtils.getBitmap(image);
                            final Bitmap bitmap = rawBitmap;
                            launchRunnable = () -> handleIngredients(fullContextText, bitmap);
                            launchHandler.postDelayed(launchRunnable, 700);
                        }
                    }
                }).addOnCompleteListener(task -> image.close());
            }
        });

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            setupTorchControl();
            isScanLocked = false;
            if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.GONE);
            if (statusTextView != null) {
                statusTextView.setText(currentMode == ScanMode.BARCODE
                        ? R.string.point_camera_barcode
                        : R.string.point_camera_ingredients);
            }
            if (scanningOverlay != null) {
                scanningOverlay.setMode(currentMode == ScanMode.BARCODE
                        ? ScanningOverlayView.OverlayMode.BARCODE
                        : ScanningOverlayView.OverlayMode.INGREDIENTS);
                scanningOverlay.startScanning();
            }
            scheduleBarcodeScanTimeout();
        } catch (Exception e) {
            showCameraUnavailable();
        }
    }

    private int getIngredientConfidence(String text) {
        String lower = text.toLowerCase();
        int score = 0;
        
        // Primary marker: The word "ingredients" is essential for this mode
        if (lower.contains("ingredients") || lower.contains("ingrédients") || lower.contains("ingredientes")) {
            score += 75;
        } else if (lower.contains("contains:") || lower.contains("contient:") || lower.contains("contiene:")) {
            score += 45;
        }
        
        String[] markers = {
                "water", "sugar", "sucrose", "syrup", "flour", "oil", "salt", "acid",
                "flavor", "gum", "lecithin", "starch", "dextrose", "maltodextrin",
                "citric", "natural flavor", "preservative", "color", "soy", "milk", "wheat"
        };
        for (String m : markers) {
            if (lower.contains(m)) score += 12;
        }
        
        score += Math.min(35, countOccurrences(lower, ',') * 4);
        score += Math.min(15, countOccurrences(lower, ';') * 5);

        String[] nutritionMarkers = {"calories", "serving size", "total fat", "cholesterol", "daily value"};
        for (String marker : nutritionMarkers) {
            if (lower.contains(marker)) score -= 20;
        }

        if (lower.length() < 18) score -= 15;
        
        return score;
    }

    private int countOccurrences(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    private void handleIngredients(String text, Bitmap bitmap) {
        handleIngredientsWithBarcode(text, bitmap, null);
    }

    private void handleIngredientsWithBarcode(String text, Bitmap bitmap, String barcode) {
        cancelBarcodeScanTimeout();
        String processedText = text;
        String lowerText = text.toLowerCase();
        
        String[] markers = {"ingredients:", "ingredients", "contains:", "ingrédients:", "ingredientes:", "carbonated water", "water,"};
        boolean foundIngredients = false;
        for (String marker : markers) {
            if (lowerText.contains(marker)) {
                foundIngredients = true;
                break;
            }
        }

        isScanLocked = true;
        
        // Trim UI noise often found when scanning a screen/webpage
        String[] uiNoise = {"share", "download", "label", "content", "uploaded", "related searches"};
        for (String noise : uiNoise) {
            int noiseIndex = processedText.toLowerCase().indexOf(noise);
            if (noiseIndex > 20) { // Only trim if it's further down
                 processedText = processedText.substring(0, noiseIndex);
            }
        }

        Intent intent = new Intent(this, IngredientAnalysisActivity.class);
        intent.putExtra(IngredientAnalysisActivity.EXTRA_INGREDIENTS_TEXT, processedText.trim());
        if (barcode != null) {
            intent.putExtra(IngredientAnalysisActivity.EXTRA_BARCODE, barcode);
        }
        if (bitmap != null) {
            byte[] bytes = BitmapUtils.bitmapToByteArray(bitmap);
            intent.putExtra("extra_image_bytes", bytes);
        }

        runOnUiThread(() -> {
            if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.VISIBLE);
            if (scanningOverlay != null) scanningOverlay.stopScanning();
        });

        // Small delay to let the loading animation show before the heavy activity transition
        launchHandler.postDelayed(() -> {
            startActivity(intent);
            runOnUiThread(() -> {
                if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.GONE);
            });
            launchHandler.postDelayed(this::resetScannerState, 3000);
        }, 300);
    }

    private void setupTorchControl() {
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
                    if (currentMode == ScanMode.INGREDIENTS) {
                        analyzeUploadedIngredientsWithBarcode(bitmap, rawValue);
                    } else {
                        handleBarcode(rawValue);
                    }
                }
            } else {
                if (currentMode == ScanMode.INGREDIENTS) {
                    analyzeUploadedIngredients(bitmap);
                } else {
                    showFailedScanRecovery();
                }
            }
        }).addOnFailureListener(e -> {
            if (currentMode == ScanMode.INGREDIENTS) {
                analyzeUploadedIngredients(bitmap);
            } else {
                showFailedScanRecovery();
            }
        });
    }

    private void analyzeUploadedIngredientsWithBarcode(Bitmap bitmap, String barcode) {
        if (bitmap == null) return;
        
        runOnUiThread(() -> {
            if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.VISIBLE);
            AiGlowManager.startGlow(this);
        });

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        textRecognizer.process(image).addOnSuccessListener(text -> {
            String fullText = text.getText();
            handleIngredientsWithBarcode(fullText.isEmpty() ? "Image upload" : fullText, bitmap, barcode);
        }).addOnFailureListener(e -> {
            handleIngredientsWithBarcode("Image upload (OCR failed)", bitmap, barcode);
        }).addOnCompleteListener(task -> {
            runOnUiThread(() -> {
                if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.GONE);
            });
        });
    }

    private void analyzeUploadedIngredients(Bitmap bitmap) {
        if (bitmap == null) return;
        
        runOnUiThread(() -> {
            if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.VISIBLE);
            AiGlowManager.startGlow(this);
        });

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        textRecognizer.process(image).addOnSuccessListener(text -> {
            String fullText = text.getText();
            handleIngredients(fullText.isEmpty() ? "Image upload" : fullText, bitmap);
        }).addOnFailureListener(e -> {
            handleIngredients("Image upload (OCR failed)", bitmap);
        }).addOnCompleteListener(task -> {
            runOnUiThread(() -> {
                if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.GONE);
            });
        });
    }

    private void handleBarcode(String barcode) {
        cancelBarcodeScanTimeout();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        ProductDetailsFragment fragment = ProductDetailsFragment.newInstance(barcode, barcodeAiEnabled);
        
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentViewDestroyed(fm, f);
                if (f == fragment) {
                    ScanMode requestedRecoveryMode = pendingRecoveryMode;
                    pendingRecoveryMode = null;
                    resetScannerState();
                    if (ContextCompat.checkSelfPermission(ScanBarcodeActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startCamera();
                    }
                    if (requestedRecoveryMode == ScanMode.INGREDIENTS) {
                        launchHandler.post(ScanBarcodeActivity.this::scanIngredientsInstead);
                    }
                    getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(this);
                }
            }
        }, false);
        
        fragment.show(getSupportFragmentManager(), fragment.getTag());
    }

    private void resetScannerState() {
        isScanLocked = false;
        launchHandler.removeCallbacks(launchRunnable);
        hideScannerState();
        AiGlowManager.stopGlow(this);
        
        // Reset to Barcode Mode after a successful scan
        if (currentMode != ScanMode.BARCODE) {
            currentMode = ScanMode.BARCODE;
            modeTextView.setText(R.string.barcode_mode);
            if (statusTextView != null) statusTextView.setText(R.string.point_camera_barcode);
            modeToggleButton.setImageResource(R.drawable.ic_scan);
            modeToggleButton.setContentDescription(getString(R.string.barcode_mode));
            if (barcodeAiToggleButton != null) barcodeAiToggleButton.setVisibility(View.VISIBLE);
            if (cameraProvider != null) {
                bindCameraUseCases(cameraProvider);
            }
        }

        if (scanningOverlay != null) {
            scanningOverlay.setMode(ScanningOverlayView.OverlayMode.BARCODE);
            scanningOverlay.clearDetections();
            scanningOverlay.startScanning();
        }
        if (statusTextView != null) statusTextView.setText(R.string.point_camera_barcode);
        scheduleBarcodeScanTimeout();
    }

    private void scheduleBarcodeScanTimeout() {
        cancelBarcodeScanTimeout();
        if (currentMode == ScanMode.BARCODE
                && hasCameraPermission()
                && !isScanLocked
                && !isScannerStateVisible()
                && !isFinishing()) {
            launchHandler.postDelayed(barcodeScanTimeoutRunnable, BARCODE_SCAN_TIMEOUT_MS);
        }
    }

    private void cancelBarcodeScanTimeout() {
        launchHandler.removeCallbacks(barcodeScanTimeoutRunnable);
    }

    private void showFailedScanRecovery() {
        if (currentMode != ScanMode.BARCODE || isFinishing()) return;
        if (statusTextView != null) statusTextView.setText(R.string.barcode_not_detected_title);
        showScannerState(
                R.string.barcode_not_detected_title,
                R.string.barcode_not_detected_message,
                R.string.try_again,
                this::retryBarcodeScan,
                R.string.scan_ingredients_instead,
                this::scanIngredientsInstead
        );
    }

    private void retryBarcodeScan() {
        hideScannerState();
        currentMode = ScanMode.BARCODE;
        isScanLocked = false;
        modeTextView.setText(R.string.barcode_mode);
        modeToggleButton.setImageResource(R.drawable.ic_scan);
        modeToggleButton.setContentDescription(getString(R.string.barcode_mode));
        if (barcodeAiToggleButton != null) barcodeAiToggleButton.setVisibility(View.VISIBLE);
        if (statusTextView != null) statusTextView.setText(R.string.point_camera_barcode);
        if (scanningOverlay != null) {
            scanningOverlay.setMode(ScanningOverlayView.OverlayMode.BARCODE);
            scanningOverlay.clearDetections();
            scanningOverlay.startScanning();
        }
        if (cameraProvider == null) startCamera();
        else scheduleBarcodeScanTimeout();
    }

    private void scanIngredientsInstead() {
        if (!hasCameraPermission()) {
            currentMode = ScanMode.INGREDIENTS;
            modeTextView.setText(R.string.ingredient_mode);
            if (statusTextView != null) statusTextView.setText(R.string.camera_permission_denied_title);
            showCameraPermissionDenied();
            openPhotoPicker();
            return;
        }
        hideScannerState();
        isScanLocked = false;
        if (currentMode == ScanMode.BARCODE) {
            toggleScanMode();
        } else if (cameraProvider != null) {
            bindCameraUseCases(cameraProvider);
        }
    }

    private void registerNetworkCallback() {
        if (connectivityManager == null || networkCallbackRegistered) {
            return;
        }
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(ScanBarcodeActivity.this::updateConnectionStatus);
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(ScanBarcodeActivity.this::updateConnectionStatus);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                runOnUiThread(ScanBarcodeActivity.this::updateConnectionStatus);
            }
        };
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
        networkCallbackRegistered = true;
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager == null || !networkCallbackRegistered || networkCallback == null) {
            return;
        }
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (IllegalArgumentException ignored) {
            // The system may already have removed the callback while the activity was paused.
        }
        networkCallbackRegistered = false;
    }

    private void updateConnectionStatus() {
        if (offlineIndicator == null) {
            return;
        }

        boolean online = NetworkUtils.isOnline(this);
        boolean connectionRestored = Boolean.FALSE.equals(lastKnownOnline) && online;
        lastKnownOnline = online;
        launchHandler.removeCallbacks(hideConnectionMessageRunnable);

        if (!online) {
            offlineIndicator.setBackgroundColor(Color.parseColor("#D32F2F"));
            offlineIndicator.setText(R.string.scanner_offline_indicator);
            offlineIndicator.setVisibility(View.VISIBLE);
            return;
        }

        if (connectionRestored) {
            offlineIndicator.setBackgroundColor(Color.parseColor("#2E7D32"));
            offlineIndicator.setText(R.string.scanner_online_restored);
            offlineIndicator.setVisibility(View.VISIBLE);
            launchHandler.postDelayed(hideConnectionMessageRunnable, 2500L);
            return;
        }

        offlineIndicator.setVisibility(View.GONE);
    }
}

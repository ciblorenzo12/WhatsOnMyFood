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
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions;
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

    public static final String EXTRA_DISABLE_SCAN_TIMEOUTS_FOR_TESTS = "extra_disable_scan_timeouts_for_tests";
    public static final String EXTRA_SUPPLEMENTAL_TARGET = "extra_supplemental_target";
    public static final String EXTRA_EXISTING_INGREDIENT_TEXT = "extra_existing_ingredient_text";
    public static final String EXTRA_EXISTING_PRODUCT_TEXT = "extra_existing_product_text";
    public static final String EXTRA_SOURCE_BARCODE = "extra_source_barcode";
    public static final String EXTRA_SUPPLEMENTAL_ATTEMPT = "extra_supplemental_attempt";
    public static final String TARGET_PRODUCT_NAME = "product_name";
    public static final String TARGET_INGREDIENTS = "ingredients";
    private static final String SCANNER_PREFERENCES = "scanner_preferences";
    private static final String CAMERA_PERMISSION_REQUESTED = "camera_permission_requested";
    static final String PRODUCT_DETAILS_TAG = "scanned_product_details";
    private static final long BARCODE_SCAN_TIMEOUT_MS = 12_000L;
    private static final long INGREDIENT_SCAN_TIMEOUT_MS = 15_000L;

    public enum ScanMode {
        BARCODE, INGREDIENTS
    }

    private enum SupplementalTarget {
        NONE, PRODUCT_NAME, INGREDIENTS
    }

    private PreviewView previewView;
    private TextView offlineIndicator, modeTextView, statusTextView, modeToggleHintText;
    private LinearLayout scannerStatePanel;
    private TextView scannerStateTitle, scannerStateMessage;
    private Button scannerStatePrimaryButton, scannerStateSecondaryButton;
    private ImageButton modeToggleButton;
    private View modeSwitchContainer;
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
    private final BarcodeScanGate barcodeScanGate = new BarcodeScanGate();
    private ScanFailureLogger scanFailureLogger;
    private boolean scanTimeoutsEnabled = true;
    private boolean uiTestMode;
    private SupplementalTarget supplementalTarget = SupplementalTarget.NONE;
    private String existingIngredientText = "";
    private String existingProductText = "";
    private String supplementalBarcode;
    private int supplementalAttempt;

    private final Handler launchHandler = new Handler(Looper.getMainLooper());
    private Runnable launchRunnable;
    private final Runnable barcodeScanTimeoutRunnable = () -> {
        if (scanFailureLogger != null) {
            scanFailureLogger.record("barcode_camera", "", "timeout", "No valid product GTIN detected within 12 seconds");
        }
        showFailedScanRecovery();
    };
    private final Runnable ingredientScanTimeoutRunnable = () -> {
        if (scanFailureLogger != null) {
            boolean productNameScan = supplementalTarget == SupplementalTarget.PRODUCT_NAME;
            scanFailureLogger.record(
                    productNameScan ? "ocr_product_name" : "ocr_camera",
                    supplementalBarcode == null ? "" : supplementalBarcode,
                    "timeout",
                    productNameScan
                            ? "No readable product name detected within 15 seconds"
                            : "No readable ingredient list detected within 15 seconds"
            );
        }
        showIngredientScanRecovery();
    };
    private AnimatorSet modeToggleGlowAnimator;
    private final Runnable hideConnectionMessageRunnable = () -> {
        if (offlineIndicator != null && NetworkUtils.isOnline(this)) {
            offlineIndicator.setVisibility(View.GONE);
        }
    };

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri != null) {
            try {
                cancelScanTimeouts();
                isScanLocked = true;
                if (cameraProvider != null) cameraProvider.unbindAll();
                barcodeScanGate.reset();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                decodeBarcodeFromBitmap(bitmap);
            } catch (IOException e) {
                scanFailureLogger.record("photo_import", "", "image_load_error", e);
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                isScanLocked = false;
                barcodeScanGate.reset();
                if (cameraProvider == null) startCamera();
                else bindCameraUseCases(cameraProvider);
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
        uiTestMode = BuildConfig.DEBUG
                && getIntent().getBooleanExtra(EXTRA_DISABLE_SCAN_TIMEOUTS_FOR_TESTS, false);
        scanTimeoutsEnabled = !uiTestMode;

        previewView = findViewById(R.id.preview_view);
        scanningOverlay = findViewById(R.id.scanning_overlay);
        offlineIndicator = findViewById(R.id.offline_indicator);
        modeTextView = findViewById(R.id.mode_text_view);
        statusTextView = findViewById(R.id.status_text_view);
        modeToggleButton = findViewById(R.id.mode_toggle_button);
        modeSwitchContainer = findViewById(R.id.mode_switch_container);
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
        scanFailureLogger = new ScanFailureLogger(this);
        readSupplementalRequest();

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
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_CODE_128)
                .enableAllPotentialBarcodes()
                .setZoomSuggestionOptions(new ZoomSuggestionOptions.Builder(this::applySuggestedZoom)
                        .setMaxSupportedZoomRatio(4f)
                        .build())
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        View.OnClickListener modeSwitchListener = v -> {
            hideModeToggleHint();
            toggleScanMode();
        };
        modeToggleButton.setOnClickListener(modeSwitchListener);
        if (modeSwitchContainer != null) modeSwitchContainer.setOnClickListener(modeSwitchListener);
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

        if (supplementalTarget != SupplementalTarget.NONE) {
            configureSupplementalMode();
            showSupplementalScanPrompt();
        } else if (!uiTestMode) {
            prepareCameraAccess();
            launchHandler.postDelayed(this::showModeToggleHintIfNeeded, 900);
        }
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
            scheduleCurrentScanTimeout();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterNetworkCallback();
        launchHandler.removeCallbacks(hideConnectionMessageRunnable);
        cancelScanTimeouts();
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
        if (barcodeScanner != null) barcodeScanner.close();
        if (textRecognizer != null) textRecognizer.close();
        launchHandler.removeCallbacksAndMessages(null);
        stopModeToggleGlow();
    }

    private void readSupplementalRequest() {
        String target = getIntent().getStringExtra(EXTRA_SUPPLEMENTAL_TARGET);
        if (TARGET_PRODUCT_NAME.equals(target)) {
            supplementalTarget = SupplementalTarget.PRODUCT_NAME;
        } else if (TARGET_INGREDIENTS.equals(target)) {
            supplementalTarget = SupplementalTarget.INGREDIENTS;
        }
        existingIngredientText = safeExtra(EXTRA_EXISTING_INGREDIENT_TEXT);
        existingProductText = safeExtra(EXTRA_EXISTING_PRODUCT_TEXT);
        supplementalBarcode = BarcodeScanGate.normalizeAndValidate(getIntent().getStringExtra(EXTRA_SOURCE_BARCODE));
        supplementalAttempt = Math.max(0, getIntent().getIntExtra(EXTRA_SUPPLEMENTAL_ATTEMPT, 0));
    }

    private String safeExtra(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value.trim();
    }

    private void configureSupplementalMode() {
        currentMode = ScanMode.INGREDIENTS;
        boolean productNameScan = supplementalTarget == SupplementalTarget.PRODUCT_NAME;
        modeTextView.setText(productNameScan ? R.string.product_name_scan_mode : R.string.ingredient_mode);
        statusTextView.setText(productNameScan ? R.string.point_camera_product_name : R.string.point_camera_ingredients);
        modeToggleButton.setImageResource(R.drawable.ic_ingredients_list);
        modeToggleButton.setContentDescription(getString(
                productNameScan ? R.string.product_name_scan_mode : R.string.ingredient_mode
        ));
        modeToggleButton.setEnabled(false);
        if (modeSwitchContainer != null) modeSwitchContainer.setEnabled(false);
        if (barcodeAiToggleButton != null) barcodeAiToggleButton.setVisibility(View.GONE);
        if (scanningOverlay != null) {
            scanningOverlay.setMode(ScanningOverlayView.OverlayMode.INGREDIENTS);
            scanningOverlay.stopScanning();
        }
    }

    private void showSupplementalScanPrompt() {
        boolean productNameScan = supplementalTarget == SupplementalTarget.PRODUCT_NAME;
        new AlertDialog.Builder(this)
                .setTitle(productNameScan
                        ? R.string.missing_product_name_scan_title
                        : R.string.missing_ingredients_scan_title)
                .setMessage(productNameScan
                        ? R.string.missing_product_name_scan_message
                        : R.string.missing_ingredients_scan_message)
                .setCancelable(false)
                .setPositiveButton(R.string.continue_to_camera, (dialog, which) -> prepareCameraAccess())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .show();
    }

    private int textScanInstructionRes() {
        return supplementalTarget == SupplementalTarget.PRODUCT_NAME
                ? R.string.point_camera_product_name
                : R.string.point_camera_ingredients;
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
        cancelScanTimeouts();
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
        if (modeToggleButton != null) modeToggleButton.setEnabled(supplementalTarget == SupplementalTarget.NONE);
        if (modeSwitchContainer != null) modeSwitchContainer.setEnabled(supplementalTarget == SupplementalTarget.NONE);
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
        if (supplementalTarget != SupplementalTarget.NONE) return;
        cancelScanTimeouts();
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
                    if (!uiTestMode) scanningOverlay.startScanning();
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
                    if (!uiTestMode) scanningOverlay.startScanning();
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
                .setTargetResolution(currentMode == ScanMode.INGREDIENTS
                        ? new Size(1920, 1080)
                        : new Size(1280, 720))
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
                        Barcode detectedBarcode = null;
                        for (Barcode candidate : barcodes) {
                            if (BarcodeScanGate.normalizeAndValidate(candidate.getRawValue()) != null) {
                                detectedBarcode = candidate;
                                break;
                            }
                        }
                        Barcode overlayBarcode = detectedBarcode != null ? detectedBarcode : barcodes.get(0);
                        String barcodeValue = detectedBarcode == null ? null : detectedBarcode.getRawValue();
                        Rect barcodeBox = overlayBarcode.getBoundingBox();
                        runOnUiThread(() -> {
                            if (scanningOverlay != null) {
                                scanningOverlay.updateBarcode(barcodeBox, barcodeValue != null);
                            }
                        });

                        if (!isScanLocked && barcodeValue != null) {
                            String acceptedBarcode = barcodeScanGate.tryAcquire(barcodeValue);
                            if (acceptedBarcode != null) {
                                cancelBarcodeScanTimeout();
                                isScanLocked = true;
                                launchRunnable = () -> handleBarcode(acceptedBarcode);
                                launchHandler.postDelayed(launchRunnable, 300);
                            }
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (scanningOverlay != null) {
                                scanningOverlay.updateBarcode(null, false);
                            }
                        });
                    }
                }).addOnFailureListener(error ->
                        scanFailureLogger.record("barcode_camera", "", "mlkit_error", error)
                ).addOnCompleteListener(task -> image.close());
            } else {
                textRecognizer.process(inputImage).addOnSuccessListener(text -> {
                    int bestTextConfidence = 0;
                    int bestIndex = -1;
                    List<Rect> textBoxes = new ArrayList<>();
                    boolean productNameScan = supplementalTarget == SupplementalTarget.PRODUCT_NAME;
                    
                    for (com.google.mlkit.vision.text.Text.TextBlock block : text.getTextBlocks()) {
                        String blockText = block.getText();
                        int textConfidence = productNameScan
                                ? ProductNameOcrValidator.validate(blockText).confidence
                                : IngredientOcrHeuristics.confidence(blockText);
                        
                        Rect blockBox = block.getBoundingBox();
                        int mappedIndex = -1;
                        if (blockBox != null) {
                            mappedIndex = textBoxes.size();
                            textBoxes.add(blockBox);
                        }
                        
                        if (textConfidence > bestTextConfidence) {
                            bestTextConfidence = textConfidence;
                            bestIndex = mappedIndex;
                        }
                    }

                    final int mappedBestIndex = bestIndex;
                    runOnUiThread(() -> {
                        if (scanningOverlay != null) {
                            scanningOverlay.updateTextBlocks(textBoxes, mappedBestIndex);
                        }
                    });

                    String fullContextText = IngredientOcrHeuristics.prepareRecognizedText(text.getText());
                    boolean readyToCapture = productNameScan
                            ? ProductNameOcrValidator.validate(fullContextText).readable
                            : bestTextConfidence > 25;
                    if (readyToCapture) {
                        if (!isScanLocked) {
                            isScanLocked = true;
                            Bitmap rawBitmap = BitmapUtils.getBitmap(image);
                            final Bitmap bitmap = rawBitmap;
                            launchRunnable = productNameScan
                                    ? () -> handleSupplementalProductName(fullContextText, bitmap, supplementalBarcode)
                                    : () -> handleIngredientsWithBarcode(fullContextText, bitmap, supplementalBarcode);
                            launchHandler.postDelayed(launchRunnable, 700);
                        }
                    }
                }).addOnFailureListener(error ->
                        scanFailureLogger.record("ocr_camera", "", "mlkit_error", error)
                ).addOnCompleteListener(task -> image.close());
            }
        });

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            setupTorchControl();
            barcodeScanGate.reset();
            isScanLocked = false;
            if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.GONE);
            if (statusTextView != null) {
                statusTextView.setText(currentMode == ScanMode.BARCODE
                        ? R.string.point_camera_barcode
                        : textScanInstructionRes());
            }
            if (scanningOverlay != null) {
                scanningOverlay.setMode(currentMode == ScanMode.BARCODE
                        ? ScanningOverlayView.OverlayMode.BARCODE
                        : ScanningOverlayView.OverlayMode.INGREDIENTS);
                scanningOverlay.startScanning();
            }
            scheduleCurrentScanTimeout();
        } catch (Exception e) {
            showCameraUnavailable();
        }
    }

    private void handleIngredientsWithBarcode(String text, Bitmap bitmap, String barcode) {
        cancelScanTimeouts();
        String processedText = IngredientOcrHeuristics.trimUiNoise(text);

        IngredientLabelValidator.Result validation = IngredientLabelValidator.validate(processedText);
        if (!validation.readable) {
            scanFailureLogger.record(
                    "ocr_label",
                    barcode == null ? "" : barcode,
                    validation.failureReason.name().toLowerCase(),
                    "OCR output did not contain a reliable ingredient list"
            );
            runOnUiThread(this::showIngredientScanRecovery);
            return;
        }
        processedText = validation.cleanedText;

        String analysisText = supplementalTarget == SupplementalTarget.INGREDIENTS
                ? SupplementalOcrMerger.merge(existingProductText, processedText)
                : processedText;
        launchIngredientAnalysis(analysisText, bitmap, barcode);
    }

    private void handleSupplementalProductName(String text, Bitmap bitmap, String barcode) {
        cancelScanTimeouts();
        ProductNameOcrValidator.Result validation = ProductNameOcrValidator.validate(text);
        if (!validation.readable) {
            scanFailureLogger.record(
                    "ocr_product_name",
                    barcode == null ? "" : barcode,
                    "no_reliable_product_name",
                    "OCR output did not contain useful front-label identity text"
            );
            runOnUiThread(this::showProductNameScanRecovery);
            return;
        }

        String analysisText = SupplementalOcrMerger.merge(validation.cleanedText, existingIngredientText);
        launchIngredientAnalysis(analysisText, bitmap, barcode);
    }

    private void launchIngredientAnalysis(String analysisText, Bitmap bitmap, String barcode) {
        if (!IngredientLabelValidator.validate(analysisText).readable) {
            runOnUiThread(this::showIngredientScanRecovery);
            return;
        }

        isScanLocked = true;
        
        Intent intent = new Intent(this, IngredientAnalysisActivity.class);
        intent.putExtra(IngredientAnalysisActivity.EXTRA_INGREDIENTS_TEXT, analysisText.trim());
        intent.putExtra(IngredientAnalysisActivity.EXTRA_SUPPLEMENTAL_ATTEMPT, supplementalAttempt);
        if (barcode != null) {
            intent.putExtra(IngredientAnalysisActivity.EXTRA_BARCODE, barcode);
        }
        if (bitmap != null) {
            byte[] bytes = BitmapUtils.bitmapToByteArray(bitmap);
            intent.putExtra(IngredientAnalysisActivity.EXTRA_IMAGE_BYTES, bytes);
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
            if (supplementalTarget != SupplementalTarget.NONE) {
                finish();
            } else {
                launchHandler.postDelayed(this::resetScannerState, 3000);
            }
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

    private boolean applySuggestedZoom(float suggestedRatio) {
        Camera activeCamera = camera;
        if (activeCamera == null) return false;
        ZoomState zoomState = activeCamera.getCameraInfo().getZoomState().getValue();
        float maxZoom = zoomState == null ? 4f : zoomState.getMaxZoomRatio();
        float minZoom = zoomState == null ? 1f : zoomState.getMinZoomRatio();
        float clampedRatio = Math.max(minZoom, Math.min(suggestedRatio, maxZoom));
        activeCamera.getCameraControl().setZoomRatio(clampedRatio);
        return true;
    }

    private void decodeBarcodeFromBitmap(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        barcodeScanner.process(image).addOnSuccessListener(barcodes -> {
            String acceptedBarcode = null;
            for (Barcode candidate : barcodes) {
                acceptedBarcode = barcodeScanGate.tryAcquire(candidate.getRawValue());
                if (acceptedBarcode != null) break;
            }
            if (acceptedBarcode != null) {
                if (currentMode == ScanMode.INGREDIENTS) {
                    String barcodeForAnalysis = supplementalBarcode == null
                            ? acceptedBarcode
                            : supplementalBarcode;
                    analyzeUploadedIngredientsWithBarcode(bitmap, barcodeForAnalysis);
                } else {
                    handleBarcode(acceptedBarcode);
                }
            } else {
                if (currentMode == ScanMode.INGREDIENTS) {
                    analyzeUploadedIngredients(bitmap);
                } else {
                    scanFailureLogger.record("barcode_photo", "", "not_detected", "No valid product GTIN found in imported image");
                    showFailedScanRecovery();
                }
            }
        }).addOnFailureListener(e -> {
            scanFailureLogger.record("barcode_photo", "", "mlkit_error", e);
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

        recognizeImportedIngredientText(bitmap, new ImportedOcrCallback() {
            @Override
            public void onSuccess(String fullText) {
                routeImportedOcr(fullText.isEmpty() ? "Image upload" : fullText, bitmap, barcode);
                hideImportedOcrProgress();
            }

            @Override
            public void onFailure(Exception error) {
                scanFailureLogger.record("ocr_photo", barcode, "original_and_enhanced_failed", error);
                routeImportedOcr("Image upload (OCR failed)", bitmap, barcode);
                hideImportedOcrProgress();
            }
        });
    }

    private void analyzeUploadedIngredients(Bitmap bitmap) {
        if (bitmap == null) return;
        
        runOnUiThread(() -> {
            if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.VISIBLE);
            AiGlowManager.startGlow(this);
        });

        recognizeImportedIngredientText(bitmap, new ImportedOcrCallback() {
            @Override
            public void onSuccess(String fullText) {
                routeImportedOcr(fullText.isEmpty() ? "Image upload" : fullText, bitmap, supplementalBarcode);
                hideImportedOcrProgress();
            }

            @Override
            public void onFailure(Exception error) {
                scanFailureLogger.record("ocr_photo", "", "original_and_enhanced_failed", error);
                routeImportedOcr("Image upload (OCR failed)", bitmap, supplementalBarcode);
                hideImportedOcrProgress();
            }
        });
    }

    private void routeImportedOcr(String text, Bitmap bitmap, String barcode) {
        if (supplementalTarget == SupplementalTarget.PRODUCT_NAME) {
            handleSupplementalProductName(text, bitmap, barcode);
        } else {
            handleIngredientsWithBarcode(text, bitmap, barcode);
        }
    }

    private void recognizeImportedIngredientText(Bitmap bitmap, ImportedOcrCallback callback) {
        Bitmap enhanced = OcrImagePreprocessor.enhance(bitmap);
        textRecognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(original -> runEnhancedOcrPass(
                        enhanced,
                        original.getText(),
                        null,
                        callback
                ))
                .addOnFailureListener(originalError -> {
                    scanFailureLogger.record("ocr_photo", "", "original_pass_error", originalError);
                    runEnhancedOcrPass(enhanced, "", originalError, callback);
                });
    }

    private void runEnhancedOcrPass(
            Bitmap enhanced,
            String originalText,
            Exception originalError,
            ImportedOcrCallback callback
    ) {
        if (enhanced == null) {
            if (!originalText.isEmpty()) {
                callback.onSuccess(IngredientOcrHeuristics.prepareRecognizedText(originalText));
            } else {
                callback.onFailure(originalError == null
                        ? new IllegalStateException("OCR preprocessing produced no image")
                        : originalError);
            }
            return;
        }

        textRecognizer.process(InputImage.fromBitmap(enhanced, 0))
                .addOnSuccessListener(enhancedResult -> callback.onSuccess(
                        IngredientOcrHeuristics.chooseBest(originalText, enhancedResult.getText())
                ))
                .addOnFailureListener(enhancedError -> {
                    scanFailureLogger.record("ocr_photo", "", "enhanced_pass_error", enhancedError);
                    if (!originalText.isEmpty()) {
                        callback.onSuccess(IngredientOcrHeuristics.prepareRecognizedText(originalText));
                    } else {
                        callback.onFailure(enhancedError);
                    }
                })
                .addOnCompleteListener(task -> enhanced.recycle());
    }

    private void hideImportedOcrProgress() {
        runOnUiThread(() -> {
            if (transitionProgressBar != null) transitionProgressBar.setVisibility(View.GONE);
        });
    }

    private interface ImportedOcrCallback {
        void onSuccess(String text);
        void onFailure(Exception error);
    }

    void handleBarcode(String barcode) {
        String validatedBarcode = BarcodeScanGate.normalizeAndValidate(barcode);
        if (validatedBarcode == null) {
            scanFailureLogger.record("barcode_navigation", barcode, "invalid_gtin", "Product details were not opened");
            showFailedScanRecovery();
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(PRODUCT_DETAILS_TAG) != null) {
            return;
        }

        cancelScanTimeouts();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        ProductDetailsFragment fragment = ProductDetailsFragment.newInstance(validatedBarcode, barcodeAiEnabled);
        
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
        
        fragment.show(fragmentManager, PRODUCT_DETAILS_TAG);
    }

    private void resetScannerState() {
        isScanLocked = false;
        barcodeScanGate.reset();
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
        if (scanTimeoutsEnabled
                && currentMode == ScanMode.BARCODE
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

    private void scheduleIngredientScanTimeout() {
        launchHandler.removeCallbacks(ingredientScanTimeoutRunnable);
        if (scanTimeoutsEnabled
                && currentMode == ScanMode.INGREDIENTS
                && hasCameraPermission()
                && !isScanLocked
                && !isScannerStateVisible()
                && !isFinishing()) {
            launchHandler.postDelayed(ingredientScanTimeoutRunnable, INGREDIENT_SCAN_TIMEOUT_MS);
        }
    }

    private void scheduleCurrentScanTimeout() {
        cancelScanTimeouts();
        if (currentMode == ScanMode.INGREDIENTS) scheduleIngredientScanTimeout();
        else scheduleBarcodeScanTimeout();
    }

    private void cancelScanTimeouts() {
        cancelBarcodeScanTimeout();
        launchHandler.removeCallbacks(ingredientScanTimeoutRunnable);
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

    private void showIngredientScanRecovery() {
        if (currentMode != ScanMode.INGREDIENTS || isFinishing()) return;
        if (supplementalTarget == SupplementalTarget.PRODUCT_NAME) {
            showProductNameScanRecovery();
            return;
        }
        if (statusTextView != null) statusTextView.setText(R.string.ingredients_not_read_title);
        showScannerState(
                R.string.ingredients_not_read_title,
                R.string.ingredients_not_read_message,
                R.string.try_again,
                this::retryIngredientScan,
                R.string.scan_from_photo,
                this::openPhotoPicker
        );
    }

    private void showProductNameScanRecovery() {
        if (currentMode != ScanMode.INGREDIENTS || isFinishing()) return;
        if (statusTextView != null) statusTextView.setText(R.string.product_name_not_read_title);
        showScannerState(
                R.string.product_name_not_read_title,
                R.string.product_name_not_read_message,
                R.string.try_again,
                this::retryIngredientScan,
                R.string.scan_from_photo,
                this::openPhotoPicker
        );
    }

    private void retryIngredientScan() {
        hideScannerState();
        currentMode = ScanMode.INGREDIENTS;
        isScanLocked = false;
        barcodeScanGate.reset();
        boolean productNameScan = supplementalTarget == SupplementalTarget.PRODUCT_NAME;
        modeTextView.setText(productNameScan ? R.string.product_name_scan_mode : R.string.ingredient_mode);
        modeToggleButton.setImageResource(R.drawable.ic_ingredients_list);
        modeToggleButton.setContentDescription(getString(
                productNameScan ? R.string.product_name_scan_mode : R.string.ingredient_mode
        ));
        modeToggleButton.setEnabled(supplementalTarget == SupplementalTarget.NONE);
        if (modeSwitchContainer != null) modeSwitchContainer.setEnabled(supplementalTarget == SupplementalTarget.NONE);
        if (barcodeAiToggleButton != null) barcodeAiToggleButton.setVisibility(View.GONE);
        if (statusTextView != null) statusTextView.setText(textScanInstructionRes());
        if (scanningOverlay != null) {
            scanningOverlay.setMode(ScanningOverlayView.OverlayMode.INGREDIENTS);
            scanningOverlay.clearDetections();
            scanningOverlay.startScanning();
        }
        if (cameraProvider == null) startCamera();
        else bindCameraUseCases(cameraProvider);
    }

    private void retryBarcodeScan() {
        hideScannerState();
        currentMode = ScanMode.BARCODE;
        isScanLocked = false;
        barcodeScanGate.reset();
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
        else bindCameraUseCases(cameraProvider);
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

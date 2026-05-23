package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;
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

    public enum ScanMode {
        BARCODE, INGREDIENTS
    }

    private PreviewView previewView;
    private TextView offlineIndicator, modeTextView;
    private ImageButton modeToggleButton;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private boolean isScanLocked = false;
    private ScanMode currentMode = ScanMode.BARCODE;
    private TextRecognizer textRecognizer;
    private BarcodeScanner barcodeScanner;

    private final Handler launchHandler = new Handler(Looper.getMainLooper());
    private Runnable launchRunnable;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                if (currentMode == ScanMode.INGREDIENTS) {
                    analyzeUploadedIngredients(bitmap);
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
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required to use the scanner.", Toast.LENGTH_LONG).show();
            finish();
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
        modeTextView = findViewById(R.id.mode_text_view);
        modeToggleButton = findViewById(R.id.mode_toggle_button);
        transitionProgressBar = findViewById(R.id.loading_progress_bar);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (scanningOverlay != null) {
            scanningOverlay.setMode(ScanningOverlayView.OverlayMode.BARCODE);
            scanningOverlay.startScanning();
        }

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        modeToggleButton.setOnClickListener(v -> toggleScanMode());

        ImageButton importButton = findViewById(R.id.import_button);
        importButton.setOnClickListener(v -> {
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOfflineStatus();
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

    private void toggleScanMode() {
        if (currentMode == ScanMode.BARCODE) {
            currentMode = ScanMode.INGREDIENTS;
            modeTextView.setText("Ingredient Mode");
            modeToggleButton.setImageResource(R.drawable.ic_ingredients_list);
            runOnUiThread(() -> {
                if (scanningOverlay != null) {
                    scanningOverlay.setMode(ScanningOverlayView.OverlayMode.INGREDIENTS);
                    scanningOverlay.startScanning();
                }
            });
        } else {
            currentMode = ScanMode.BARCODE;
            modeTextView.setText("Barcode Mode");
            modeToggleButton.setImageResource(R.drawable.ic_scan);
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
                runOnUiThread(() -> {
                    if (scanningOverlay != null) {
                        scanningOverlay.setMode(ScanningOverlayView.OverlayMode.BARCODE);
                        scanningOverlay.startScanning();
                    }
                });
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
                runOnUiThread(() -> {
                    if (scanningOverlay != null) {
                        scanningOverlay.setMode(ScanningOverlayView.OverlayMode.INGREDIENTS);
                        scanningOverlay.startScanning();
                    }
                });
                textRecognizer.process(inputImage).addOnSuccessListener(text -> {
                    String bestBlockText = "";
                    int bestConfidence = 0;
                    int bestIndex = -1;
                    List<Rect> textBoxes = new ArrayList<>();
                    
                    for (com.google.mlkit.vision.text.Text.TextBlock block : text.getTextBlocks()) {
                        String blockText = block.getText();
                        int confidence = getIngredientConfidence(blockText);
                        
                        // Also check for product title confidence to help recognize the item from the front
                        int titleConfidence = getProductTitleConfidence(blockText);
                        int effectiveConfidence = Math.max(confidence, titleConfidence);
                        
                        Rect blockBox = block.getBoundingBox();
                        int mappedIndex = -1;
                        if (blockBox != null) {
                            mappedIndex = textBoxes.size();
                            textBoxes.add(blockBox);
                        }
                        
                        if (effectiveConfidence > bestConfidence) {
                            bestConfidence = effectiveConfidence;
                            bestBlockText = blockText;
                            bestIndex = mappedIndex;
                        }
                    }

                    final int mappedBestIndex = bestIndex;
                    runOnUiThread(() -> {
                        if (scanningOverlay != null) {
                            scanningOverlay.updateTextBlocks(textBoxes, mappedBestIndex);
                        }
                    });

                    // Trigger if we have enough confidence in ingredients OR product title
                    if (bestConfidence > 25) {
                        if (!isScanLocked) {
                            isScanLocked = true;
                            // Pass all detected text to give the AI maximum context (ingredients + front branding)
                            final String fullContextText = text.getText();
                            Bitmap rawBitmap = BitmapUtils.getBitmap(image);
                            // Use a much larger crop (90%) to ensure front of pack branding is visible to the AI
                            final Bitmap bitmap = (rawBitmap != null) ? BitmapUtils.cropCenter(rawBitmap, 0.90f) : null;
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
        } catch (Exception e) {
            e.printStackTrace();
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

    private int getProductTitleConfidence(String text) {
        if (text == null || text.length() < 3) return 0;
        String lower = text.toLowerCase();
        int score = 0;

        // Common product attributes found on the front
        String[] frontKeywords = {"organic", "natural", "premium", "original", "classic", "roasted", "salted", "unsweetened", "flavored"};
        for (String kw : frontKeywords) {
            if (lower.contains(kw)) score += 15;
        }

        // Product names are often capitalized
        int caps = 0;
        for (char c : text.toCharArray()) if (Character.isUpperCase(c)) caps++;
        if (text.length() > 5 && (float)caps / text.length() > 0.3f) score += 25;

        // Short lines are more likely to be titles than ingredient lists
        if (text.length() < 60 && !text.contains(",")) score += 15;

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
        String processedText = text;
        String lowerText = text.toLowerCase();
        
        // We keep the full text to allow the AI to see product names/branding before the ingredients.
        // But we still want to ensure we've captured the core ingredients list.
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
                    Toast.makeText(this, "No barcode found in image", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> {
            if (currentMode == ScanMode.INGREDIENTS) {
                analyzeUploadedIngredients(bitmap);
            } else {
                Toast.makeText(this, "Error scanning image", Toast.LENGTH_SHORT).show();
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
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        ProductDetailsFragment fragment = ProductDetailsFragment.newInstance(barcode);
        
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentViewDestroyed(fm, f);
                if (f == fragment) {
                    resetScannerState();
                    if (ContextCompat.checkSelfPermission(ScanBarcodeActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startCamera();
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
        AiGlowManager.stopGlow(this);
        
        // Reset to Barcode Mode after a successful scan
        if (currentMode != ScanMode.BARCODE) {
            currentMode = ScanMode.BARCODE;
            modeTextView.setText("Barcode Mode");
            modeToggleButton.setImageResource(R.drawable.ic_scan);
            if (cameraProvider != null) {
                bindCameraUseCases(cameraProvider);
            }
        }

        if (scanningOverlay != null) {
            scanningOverlay.setMode(ScanningOverlayView.OverlayMode.BARCODE);
            scanningOverlay.clearDetections();
            scanningOverlay.startScanning();
        }
    }

    private void checkOfflineStatus(){
        if (offlineIndicator != null) {
            offlineIndicator.setVisibility(NetworkUtils.isOnline(this) ? View.GONE : View.VISIBLE);
        }
    }
}

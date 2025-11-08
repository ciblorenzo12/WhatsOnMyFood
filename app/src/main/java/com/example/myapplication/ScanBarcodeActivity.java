package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanBarcodeActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeView;
    private boolean isScanned = false;
    private ExecutorService backgroundExecutor;
    private MultiFormatReader multiFormatReader;

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    initializeScanner();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan barcodes", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    backgroundExecutor.execute(() -> decodeBarcodeFromUri(uri));
                }
            });

    private final ActivityResultLauncher<String> requestStoragePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    pickImageLauncher.launch("image/*");
                } else {
                    Toast.makeText(this, "Storage permission is required to import images", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        backgroundExecutor = Executors.newSingleThreadExecutor();
        setupBarcodeReader();

        barcodeView = findViewById(R.id.zxing_barcode_scanner);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initializeScanner();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        ImageButton importButton = findViewById(R.id.import_button);
        importButton.setOnClickListener(v -> {
            String permission = Manifest.permission.READ_MEDIA_IMAGES;
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                requestStoragePermissionLauncher.launch(permission);
            }
        });
    }

    private void setupBarcodeReader() {
        multiFormatReader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        multiFormatReader.setHints(hints);
    }

    private void initializeScanner() {
        barcodeView.decodeContinuous(callback);

        ToggleButton torchButton = findViewById(R.id.torch_button);
        if (!hasFlash()) {
            torchButton.setVisibility(View.GONE);
        } else {
            torchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    barcodeView.setTorchOn();
                } else {
                    barcodeView.setTorchOff();
                }
            });
        }
    }

    private boolean hasFlash() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private final BarcodeCallback callback = result -> {
        if (result.getText() != null && !isScanned) {
            handleBarcode(result.getText());
        }
    };

    private void handleBarcode(String barcode) {
        if (isScanned) return;
        isScanned = true;
        runOnUiThread(() -> {
            barcodeView.pause();
            performHapticFeedback();
            Toast.makeText(ScanBarcodeActivity.this, "Scanned: " + barcode, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdown();
    }

    private void decodeBarcodeFromUri(Uri imageUri) {
        try {
            Bitmap originalBitmap = getScaledBitmap(imageUri);
            if (originalBitmap == null) {
                runOnUiThread(() -> Toast.makeText(this, "Could not read the image file.", Toast.LENGTH_LONG).show());
                return;
            }

            Result result = null;
            for (int i = 0; i < 4; i++) {
                int rotation = i * 90;
                Bitmap toScan = originalBitmap;
                if (rotation > 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotation);
                    toScan = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                }

                result = findBarcodeInBitmap(toScan);
                if (result != null) {
                    break;
                }
            }

            if (result != null) {
                handleBarcode(result.getText());
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Could not find a barcode in the selected image.", Toast.LENGTH_LONG).show());
            }

        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(this, "An error occurred while reading the image.", Toast.LENGTH_LONG).show());
        }
    }

    private Result findBarcodeInBitmap(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), pixels);
        Result result = null;

        try {
            result = multiFormatReader.decode(new BinaryBitmap(new HybridBinarizer(source)));
        } catch (NotFoundException e) { /* continue */ }

        if (result == null) {
            try {
                result = multiFormatReader.decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
            } catch (NotFoundException e) { /* continue */ }
        }

        if (result == null) {
            LuminanceSource invertedSource = source.invert();
            try {
                result = multiFormatReader.decode(new BinaryBitmap(new HybridBinarizer(invertedSource)));
            } catch (NotFoundException e) { /* continue */ }

            if (result == null) {
                try {
                    result = multiFormatReader.decode(new BinaryBitmap(new GlobalHistogramBinarizer(invertedSource)));
                } catch (NotFoundException e) { /* last attempt failed */ }
            }
        }
        return result;
    }

    private Bitmap getScaledBitmap(Uri imageUri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            if (inputStream == null) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream);

            options.inSampleSize = calculateInSampleSize(options, 1200, 1200);
            options.inJustDecodeBounds = false;

            try (InputStream newInputStream = getContentResolver().openInputStream(imageUri)){
                return BitmapFactory.decodeStream(newInputStream, null, options);
            }
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void performHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                vibratorManager.getDefaultVibrator().vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }
}

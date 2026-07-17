package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Appends scanner, OCR, and product lookup failures to an internal CSV audit trail. */
public final class ScanFailureLogger {

    public static final String FILE_NAME = "scanner_failures.csv";
    private static final String TAG = "ScanFailureLogger";
    private final File outputFile;

    public ScanFailureLogger(Context context) {
        outputFile = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
    }

    public synchronized void record(String stage, String productCode, String condition, Throwable error) {
        String message = error == null ? "" : error.getClass().getSimpleName() + ": " + error.getMessage();
        record(stage, productCode, condition, message);
    }

    public synchronized void record(String stage, String productCode, String condition, String detail) {
        try {
            boolean addHeader = !outputFile.exists() || outputFile.length() == 0;
            try (FileOutputStream stream = new FileOutputStream(outputFile, true)) {
                if (addHeader) {
                    stream.write("timestamp_utc,stage,product_code,condition,error_detail\n".getBytes(StandardCharsets.UTF_8));
                }
                String row = csv(timestampUtc()) + ',' + csv(stage) + ',' + csv(productCode) + ','
                        + csv(condition) + ',' + csv(detail) + '\n';
                stream.write(row.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to write scanner failure record", e);
        }
    }

    public File getOutputFile() {
        return outputFile;
    }

    private static String timestampUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }
}

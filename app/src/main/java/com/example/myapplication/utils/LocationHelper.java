package com.example.myapplication.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.List;
import java.util.Locale;

public class LocationHelper {

    public interface LocationListener {
        void onLocationFound(double lat, double lng, String zipCode);
        void onLocationError(String error);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public static boolean hasPermissions(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public void getLastLocation(LocationListener listener) {
        if (!hasPermissions(context)) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        processLocation(location, listener);
                    } else {
                        getCachedOrRequestLocation(listener);
                    }
                })
                .addOnFailureListener(e -> getCachedOrRequestLocation(listener));
    }

    @SuppressLint("MissingPermission")
    private void getCachedOrRequestLocation(LocationListener listener) {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                processLocation(location, listener);
            } else {
                requestNewLocation(listener);
            }
        }).addOnFailureListener(e -> requestNewLocation(listener));
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocation(LocationListener listener) {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    processLocation(locationResult.getLastLocation(), listener);
                } else {
                    listener.onLocationError("Could not retrieve location");
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        }, Looper.getMainLooper());
    }

    private void processLocation(Location location, LocationListener listener) {
        String zipCode = getZipCode(location.getLatitude(), location.getLongitude());
        listener.onLocationFound(location.getLatitude(), location.getLongitude(), zipCode);
    }

    private String getZipCode(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getPostalCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

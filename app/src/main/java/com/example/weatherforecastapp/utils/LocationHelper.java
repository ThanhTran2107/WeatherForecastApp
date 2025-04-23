package com.example.weatherforecastapp.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.example.weatherforecastapp.models.LocationInfo;

public class LocationHelper {

    private static FusedLocationProviderClient fusedLocationClient;

    public interface LocationCallbackListener {
        void onLocationReceived(LocationInfo locationInfo);
    }

    @SuppressLint("MissingPermission")
    public static void getCurrentLocation(Context context, LocationCallbackListener listener) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Bạn chưa cấp quyền vị trí cho ứng dụng", Toast.LENGTH_SHORT).show();
            listener.onLocationReceived(null);
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000)
                .setNumUpdates(1);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    listener.onLocationReceived(new LocationInfo(location.getLatitude(), location.getLongitude()));
                } else {
                    Toast.makeText(context, "Không thể lấy được vị trí hiện tại. Hãy bật GPS/Wi-Fi!", Toast.LENGTH_LONG).show();
                    listener.onLocationReceived(null);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

}

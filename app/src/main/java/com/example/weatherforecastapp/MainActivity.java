package com.example.weatherforecastapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.weatherforecastapp.utils.LocationHelper;
import com.example.weatherforecastapp.view.WeatherFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> locationPermissionRequest;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        checkGpsAndGetLocation();
                    } else {
                        Toast.makeText(this, "Vui lòng cấp quyền vị trí!", Toast.LENGTH_SHORT).show();
                    }
                });

        requestLocationPermissionIfNeeded();
    }

    private void requestLocationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            checkGpsAndGetLocation();
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void checkGpsAndGetLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS chưa bật. Vui lòng bật GPS!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else {
            getUserLocation();
        }
    }

    private void getUserLocation() {
        progressBar.setVisibility(View.VISIBLE);
        LocationHelper.getCurrentLocation(this, locationInfo -> {
            progressBar.setVisibility(View.GONE);
            if (locationInfo != null) {
                WeatherFragment weatherFragment = WeatherFragment.newInstance(locationInfo);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, weatherFragment)
                        .commit();
            } else {
                Toast.makeText(this, "Không thể lấy vị trí. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

package com.example.weatherforecastapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.weatherforecastapp.models.LocationInfo;
import com.example.weatherforecastapp.utils.Constants;
import com.example.weatherforecastapp.utils.LocationHelper;
import com.example.weatherforecastapp.utils.WeatherNotificationHelper;
import com.example.weatherforecastapp.view.ForecastFragment;
import com.example.weatherforecastapp.view.MapFragment;
import com.example.weatherforecastapp.view.WeatherFragment;
import com.example.weatherforecastapp.workers.WeatherCheckWorker;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> locationPermissionRequest;
    private ActivityResultLauncher<String> notificationPermissionRequest;

    private ProgressBar progressBar;
    private Button btnShowMap;
    private Button btnShowForecast;
    private FrameLayout fragmentContainer;
    private LocationInfo currentLocationInfo;
    private static final String WEATHER_CHECK_WORK_TAG = "periodic_weather_check";
    private static final String TAG = "MainActivity";
    private boolean isLocationPermissionGranted = false;
    private boolean isNotificationPermissionRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate started");

        // Khởi tạo các view
        progressBar = findViewById(R.id.progressBar);
        btnShowMap = findViewById(R.id.btnShowMap);
        btnShowForecast = findViewById(R.id.btnShowForecast);
        fragmentContainer = findViewById(R.id.fragmentContainer);

        // Kiểm tra view
        if (progressBar == null || btnShowMap == null || btnShowForecast == null || fragmentContainer == null) {
            Log.e(TAG, "One or more views are missing in activity_main.xml!");
            Toast.makeText(this, "Lỗi giao diện, vui lòng kiểm tra layout", Toast.LENGTH_LONG).show();
            return;
        } else {
            btnShowMap.setVisibility(View.GONE);
            btnShowForecast.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        }

        // Tạo kênh thông báo
        WeatherNotificationHelper.createNotificationChannel(this);
        Log.d(TAG, "Notification channel creation requested.");

        // Đăng ký launcher quyền vị trí
        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    isLocationPermissionGranted = isGranted;
                    if (isGranted) {
                        Log.i(TAG, "Location permission GRANTED.");
                        checkGpsAndGetLocation();
                    } else {
                        Log.w(TAG, "Location permission DENIED.");
                        Toast.makeText(this, "Ứng dụng cần quyền vị trí để hiển thị thời tiết và bản đồ.", Toast.LENGTH_LONG).show();
                    }
                });

        // Đăng ký launcher quyền thông báo
        notificationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        Log.i(TAG, "Notification permission GRANTED.");
                        if (currentLocationInfo != null) {
                            startWeatherCheckWorker(currentLocationInfo.getLatitude(), currentLocationInfo.getLongitude());
                        }
                    } else {
                        Log.w(TAG, "Notification permission DENIED.");
                        Toast.makeText(this, "Bạn sẽ không nhận được cảnh báo thời tiết.", Toast.LENGTH_SHORT).show();
                        if (currentLocationInfo != null) {
                            startWeatherCheckWorker(currentLocationInfo.getLatitude(), currentLocationInfo.getLongitude());
                        }
                    }
                });

        // Xử lý click nút xem bản đồ
        btnShowMap.setOnClickListener(v -> {
            if (currentLocationInfo != null) {
                Log.d(TAG, "Navigating to MapFragment with location: " + currentLocationInfo.getLatitude() + ", " + currentLocationInfo.getLongitude());
                MapFragment mapFragment = MapFragment.newInstance(currentLocationInfo);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, mapFragment)
                        .addToBackStack("weather_fragment")
                        .commit();
            } else {
                Log.w(TAG, "Map button clicked but currentLocationInfo is null.");
                Toast.makeText(this, "Chưa lấy được vị trí để hiển thị bản đồ.", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý click nút dự báo
        btnShowForecast.setOnClickListener(v -> {
            if (currentLocationInfo != null) {
                // Kiểm tra Fragment hiện tại
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (currentFragment instanceof ForecastFragment) {
                    // Đang ở ForecastFragment, chuyển về WeatherFragment
                    Log.d(TAG, "Navigating back to WeatherFragment");
                    WeatherFragment weatherFragment = WeatherFragment.newInstance(currentLocationInfo);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, weatherFragment)
                            .commit();
                } else {
                    // Không ở ForecastFragment, chuyển sang ForecastFragment
                    Log.d(TAG, "Navigating to ForecastFragment with location: " + currentLocationInfo.getLatitude() + ", " + currentLocationInfo.getLongitude());
                    ForecastFragment forecastFragment = ForecastFragment.newInstance(currentLocationInfo);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, forecastFragment)
                            .addToBackStack("weather_fragment")
                            .commit();
                }
            } else {
                Log.w(TAG, "Forecast button clicked but currentLocationInfo is null.");
                Toast.makeText(this, "Chưa lấy được vị trí để hiển thị dự báo.", Toast.LENGTH_SHORT).show();
            }
        });

        // Yêu cầu quyền vị trí
        requestLocationPermissionIfNeeded();
        Log.d(TAG, "onCreate finished");
    }

    private void requestLocationPermissionIfNeeded() {
        Log.d(TAG, "Checking location permission...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission already granted.");
            isLocationPermissionGranted = true;
            checkGpsAndGetLocation();
        } else {
            Log.d(TAG, "Requesting location permission...");
            isLocationPermissionGranted = false;
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationPermissionRequested) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting notification permission...");
                isNotificationPermissionRequested = true;
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "Notification permission already granted.");
                if (currentLocationInfo != null) {
                    startWeatherCheckWorker(currentLocationInfo.getLatitude(), currentLocationInfo.getLongitude());
                }
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && currentLocationInfo != null) {
            Log.d(TAG, "No need for notification permission (API < 33). Starting worker.");
            startWeatherCheckWorker(currentLocationInfo.getLatitude(), currentLocationInfo.getLongitude());
        }
    }

    private void checkGpsAndGetLocation() {
        if (!isLocationPermissionGranted) {
            Log.w(TAG, "Cannot check GPS or get location without permission.");
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d(TAG, "Checking GPS status. Enabled: " + isGpsEnabled);

        if (!isGpsEnabled) {
            Toast.makeText(this, "GPS chưa bật. Vui lòng bật GPS!", Toast.LENGTH_LONG).show();
            try {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.e(TAG, "No Activity found to handle ACTION_LOCATION_SOURCE_SETTINGS");
                    Toast.makeText(this, "Không thể mở cài đặt vị trí.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening location settings", e);
                Toast.makeText(this, "Không thể mở cài đặt vị trí.", Toast.LENGTH_SHORT).show();
            }
        } else {
            getUserLocation();
        }
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission lost before calling LocationHelper!");
            Toast.makeText(this, "Mất quyền vị trí!", Toast.LENGTH_SHORT).show();
            isLocationPermissionGranted = false;
            return;
        }

        Log.d(TAG, "Starting to get user location...");
        progressBar.setVisibility(View.VISIBLE);
        btnShowMap.setVisibility(View.GONE);
        btnShowForecast.setVisibility(View.GONE);

        LocationHelper.getCurrentLocation(this, locationInfo -> {
            progressBar.setVisibility(View.GONE);
            if (locationInfo != null) {
                currentLocationInfo = locationInfo;
                Log.i(TAG, "Location obtained: Lat=" + locationInfo.getLatitude() + ", Lon=" + currentLocationInfo.getLongitude());

                WeatherFragment weatherFragment = WeatherFragment.newInstance(locationInfo);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, weatherFragment)
                        .commitAllowingStateLoss();

                btnShowMap.setVisibility(View.VISIBLE);
                btnShowForecast.setVisibility(View.VISIBLE);

                requestNotificationPermissionIfNeeded();
            } else {
                Log.e(TAG, "Failed to get location from LocationHelper.");
                Toast.makeText(this, "Không thể lấy vị trí hiện tại. Vui lòng thử lại!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startWeatherCheckWorker(double latitude, double longitude) {
        Log.i(TAG, "Attempting to start WeatherCheckWorker for Lat=" + latitude + ", Lon=" + longitude);

        boolean hasNotificationPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot schedule worker effectively without notification permission.");
                hasNotificationPermission = false;
            }
        }

        Data inputData = new Data.Builder()
                .putDouble(WeatherCheckWorker.KEY_LATITUDE, latitude)
                .putDouble(WeatherCheckWorker.KEY_LONGITUDE, longitude)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest weatherCheckRequest =
                new PeriodicWorkRequest.Builder(WeatherCheckWorker.class, 1, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setInputData(inputData)
                        .addTag(WEATHER_CHECK_WORK_TAG)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WEATHER_CHECK_WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                weatherCheckRequest);

        Log.i(TAG, "WeatherCheckWorker enqueued with policy REPLACE. Will run approx every 1 hour.");
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.w(TAG, "Worker enqueued, but notifications may not be shown due to missing permission.");
        }
    }

    private void cancelWeatherCheckWorker() {
        WorkManager.getInstance(this).cancelUniqueWork(WEATHER_CHECK_WORK_TAG);
        Log.i(TAG, "Cancelled WeatherCheckWorker.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");
        if (isLocationPermissionGranted) {
            Log.d(TAG, "Checking GPS and location again onResume.");
            checkGpsAndGetLocation();
        } else {
            Log.d(TAG, "Skipping GPS check onResume as location permission is not granted.");
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called.");
        super.onDestroy();
    }
}
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
import android.widget.FrameLayout; // Import FrameLayout nếu dùng
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
// import androidx.fragment.app.FragmentContainerView; // Import nếu dùng
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.weatherforecastapp.models.LocationInfo;
import com.example.weatherforecastapp.utils.Constants; // Import Constants
import com.example.weatherforecastapp.utils.LocationHelper;
import com.example.weatherforecastapp.utils.WeatherNotificationHelper;
import com.example.weatherforecastapp.view.MapFragment;
import com.example.weatherforecastapp.view.WeatherFragment;
import com.example.weatherforecastapp.workers.WeatherCheckWorker;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> locationPermissionRequest;
    private ActivityResultLauncher<String> notificationPermissionRequest;

    private ProgressBar progressBar;
    private Button btnShowMap;
    private FrameLayout fragmentContainer; // Hoặc FragmentContainerView
    private LocationInfo currentLocationInfo;
    private static final String WEATHER_CHECK_WORK_TAG = "periodic_weather_check";
    private static final String TAG = "MainActivity";
    private boolean isLocationPermissionGranted = false; // Cờ theo dõi quyền vị trí
    private boolean isNotificationPermissionRequested = false; // Cờ tránh hỏi quyền thông báo nhiều lần

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Đảm bảo layout này có đủ các view cần thiết

        Log.d(TAG, "onCreate started");

        progressBar = findViewById(R.id.progressBar);
        btnShowMap = findViewById(R.id.btnShowMap);
        fragmentContainer = findViewById(R.id.fragmentContainer); // Lấy container

        // Kiểm tra xem các view có tồn tại không
        if (progressBar == null || btnShowMap == null || fragmentContainer == null) {
            Log.e(TAG, "One or more views are missing in activity_main.xml!");
            Toast.makeText(this, "Lỗi giao diện, vui lòng kiểm tra layout", Toast.LENGTH_LONG).show();
            // Có thể finish() activity nếu lỗi nghiêm trọng
            // finish();
            // return;
        } else {
            btnShowMap.setVisibility(View.GONE); // Ẩn nút ban đầu
            progressBar.setVisibility(View.GONE); // Ẩn progress bar ban đầu
        }


        // 1. Tạo kênh thông báo (an toàn khi gọi nhiều lần)
        WeatherNotificationHelper.createNotificationChannel(this);
        Log.d(TAG, "Notification channel creation requested.");

        // 2. Đăng ký launcher cho quyền vị trí
        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    isLocationPermissionGranted = isGranted; // Cập nhật cờ
                    if (isGranted) {
                        Log.i(TAG, "Location permission GRANTED.");
                        checkGpsAndGetLocation();
                    } else {
                        Log.w(TAG, "Location permission DENIED.");
                        Toast.makeText(this, "Ứng dụng cần quyền vị trí để hiển thị thời tiết và bản đồ.", Toast.LENGTH_LONG).show();
                        // Hiển thị giao diện mặc định hoặc yêu cầu lại quyền với giải thích rõ hơn
                        // Ví dụ: Hiển thị một fragment giải thích
                    }
                });

        // 3. Đăng ký launcher cho quyền thông báo (Android 13+)
        notificationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        Log.i(TAG, "Notification permission GRANTED.");
                        // Nếu đã có vị trí, bắt đầu Worker ngay
                        if (currentLocationInfo != null) {
                            startWeatherCheckWorker(currentLocationInfo.getLatitude(), currentLocationInfo.getLongitude());
                        }
                    } else {
                        Log.w(TAG, "Notification permission DENIED.");
                        Toast.makeText(this, "Bạn sẽ không nhận được cảnh báo thời tiết.", Toast.LENGTH_SHORT).show();
                        // Vẫn có thể bắt đầu worker, nhưng nó sẽ không gửi được thông báo
                        if (currentLocationInfo != null) {
                            startWeatherCheckWorker(currentLocationInfo.getLatitude(), currentLocationInfo.getLongitude());
                        }
                    }
                });


        // 4. Yêu cầu quyền vị trí khi activity được tạo
        requestLocationPermissionIfNeeded();

        // 5. Xử lý sự kiện click nút xem bản đồ
        if (btnShowMap != null) {
            btnShowMap.setOnClickListener(v -> {
                if (currentLocationInfo != null) {
                    Log.d(TAG, "Navigating to MapFragment with location: " + currentLocationInfo.getLatitude() + ", " + currentLocationInfo.getLongitude());
                    MapFragment mapFragment = MapFragment.newInstance(currentLocationInfo);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, mapFragment)
                            .addToBackStack("weather_fragment") // Cho phép quay lại WeatherFragment
                            .commit();
                } else {
                    Log.w(TAG, "Map button clicked but currentLocationInfo is null.");
                    Toast.makeText(this, "Chưa lấy được vị trí để hiển thị bản đồ.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        Log.d(TAG, "onCreate finished");
    }

    private void requestLocationPermissionIfNeeded() {
        Log.d(TAG, "Checking location permission...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission already granted.");
            isLocationPermissionGranted = true;
            checkGpsAndGetLocation(); // Nếu đã có quyền, kiểm tra GPS và lấy vị trí
        } else {
            // Quyền chưa được cấp, yêu cầu người dùng
            Log.d(TAG, "Requesting location permission...");
            isLocationPermissionGranted = false;
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        // Chỉ yêu cầu nếu là Android 13+ và chưa yêu cầu trước đó trong phiên này
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationPermissionRequested) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting notification permission...");
                isNotificationPermissionRequested = true; // Đánh dấu đã yêu cầu
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "Notification permission already granted.");
                // Quyền đã có, có thể bắt đầu Worker nếu vị trí đã sẵn sàng
                if (currentLocationInfo != null) {
                    startWeatherCheckWorker(currentLocationInfo.getLatitude(), currentLocationInfo.getLongitude());
                }
            }
        } else {
            // Dưới Android 13 hoặc đã yêu cầu rồi, không cần làm gì thêm ở đây
            // Nếu dưới Android 13, Worker có thể bắt đầu ngay nếu có vị trí
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && currentLocationInfo != null) {
                Log.d(TAG, "No need for notification permission (API < 33) or already requested. Starting worker if location available.");
                startWeatherCheckWorker(currentLocationInfo.getLatitude(), currentLocationInfo.getLongitude());
            }
        }
    }


    private void checkGpsAndGetLocation() {
        // Chỉ tiếp tục nếu có quyền vị trí
        if (!isLocationPermissionGranted) {
            Log.w(TAG, "Cannot check GPS or get location without permission.");
            // Có thể yêu cầu lại quyền ở đây nếu muốn
            // requestLocationPermissionIfNeeded();
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d(TAG, "Checking GPS status. Enabled: " + isGpsEnabled);

        if (!isGpsEnabled) {
            Toast.makeText(this, "GPS chưa bật. Vui lòng bật GPS!", Toast.LENGTH_LONG).show();
            try {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                // Kiểm tra xem có Activity nào xử lý Intent này không
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
            // Người dùng cần quay lại ứng dụng sau khi bật GPS.
            // onResume sẽ được gọi và có thể kiểm tra lại.
        } else {
            // GPS đã bật, tiến hành lấy vị trí
            getUserLocation();
        }
    }

    private void getUserLocation() {
        // Kiểm tra lại quyền lần cuối trước khi gọi LocationHelper
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission lost before calling LocationHelper!");
            Toast.makeText(this, "Mất quyền vị trí!", Toast.LENGTH_SHORT).show();
            isLocationPermissionGranted = false; // Cập nhật lại cờ
            // Có thể yêu cầu lại quyền
            // requestLocationPermissionIfNeeded();
            return;
        }

        Log.d(TAG, "Starting to get user location...");
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (btnShowMap != null) btnShowMap.setVisibility(View.GONE); // Ẩn nút trong khi tải

        LocationHelper.getCurrentLocation(this, locationInfo -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (locationInfo != null) {
                currentLocationInfo = locationInfo; // Lưu lại vị trí
                Log.i(TAG, "Location obtained: Lat=" + locationInfo.getLatitude() + ", Lon=" + locationInfo.getLongitude());

                // Hiển thị WeatherFragment
                WeatherFragment weatherFragment = WeatherFragment.newInstance(locationInfo);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, weatherFragment)
                        // .commit(); // Sử dụng commit nếu chắc chắn không gọi sau onSaveInstanceState
                        .commitAllowingStateLoss(); // An toàn hơn nếu có thể gọi muộn

                if (btnShowMap != null) btnShowMap.setVisibility(View.VISIBLE); // Hiển thị nút xem bản đồ

                // Sau khi có vị trí, yêu cầu quyền thông báo (nếu cần) và khởi động Worker
                requestNotificationPermissionIfNeeded(); // Hàm này sẽ gọi startWorker nếu có quyền

            } else {
                Log.e(TAG, "Failed to get location from LocationHelper.");
                Toast.makeText(this, "Không thể lấy vị trí hiện tại. Vui lòng thử lại!", Toast.LENGTH_LONG).show();
                // Hiển thị thông báo lỗi hoặc trạng thái mặc định
                // Ví dụ: Hiển thị một fragment báo lỗi
            }
        });
    }

    /**
     * Khởi động Worker kiểm tra thời tiết định kỳ.
     * @param latitude Vĩ độ hiện tại.
     * @param longitude Kinh độ hiện tại.
     */
    private void startWeatherCheckWorker(double latitude, double longitude) {
        Log.i(TAG, "Attempting to start WeatherCheckWorker for Lat=" + latitude + ", Lon=" + longitude);

        // Kiểm tra lại quyền thông báo một lần nữa trước khi lập lịch
        boolean hasNotificationPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot schedule worker effectively without notification permission.");
                hasNotificationPermission = false;
                // Không cần Toast ở đây vì đã có trong requestNotificationPermissionIfNeeded
            }
        }

        // Mặc dù worker vẫn có thể chạy mà không cần quyền thông báo,
        // việc kiểm tra ở đây giúp ghi log rõ ràng hơn.

        // Tạo dữ liệu đầu vào cho Worker
        Data inputData = new Data.Builder()
                .putDouble(WeatherCheckWorker.KEY_LATITUDE, latitude)
                .putDouble(WeatherCheckWorker.KEY_LONGITUDE, longitude)
                .build();

        // Đặt ràng buộc cho Worker (ví dụ: cần có mạng)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Chỉ chạy khi có kết nối mạng
                // .setRequiresBatteryNotLow(true) // Tùy chọn: Chỉ chạy khi pin không yếu
                .build();

        // Tạo yêu cầu công việc định kỳ (ví dụ: chạy mỗi 1 giờ)
        // Lưu ý: Interval tối thiểu của PeriodicWorkRequest là 15 phút.
        PeriodicWorkRequest weatherCheckRequest =
                new PeriodicWorkRequest.Builder(WeatherCheckWorker.class,
                        1, TimeUnit.HOURS) // Chạy mỗi 1 giờ
                        .setConstraints(constraints)
                        .setInputData(inputData)
                        .addTag(WEATHER_CHECK_WORK_TAG)
                        // .setInitialDelay(5, TimeUnit.MINUTES) // Tùy chọn: trì hoãn lần chạy đầu tiên
                        .build();

        // Đưa yêu cầu vào hàng đợi WorkManager
        // Sử dụng REPLACE để đảm bảo worker luôn sử dụng vị trí mới nhất nếu app khởi động lại
        // Sử dụng KEEP nếu bạn chỉ muốn có 1 worker chạy và không cần cập nhật vị trí thường xuyên
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WEATHER_CHECK_WORK_TAG, // Tên duy nhất cho công việc
                ExistingPeriodicWorkPolicy.REPLACE, // Thay thế công việc cũ bằng công việc mới (cập nhật vị trí)
                // ExistingPeriodicWorkPolicy.KEEP, // Giữ lại công việc cũ nếu đã tồn tại
                weatherCheckRequest);

        Log.i(TAG, "WeatherCheckWorker enqueued with policy REPLACE. Will run approx every 1 hour.");
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.w(TAG, "Worker enqueued, but notifications may not be shown due to missing permission.");
        }
        // Toast.makeText(this, "Đã bật kiểm tra thời tiết nền.", Toast.LENGTH_SHORT).show(); // Thông báo (tùy chọn)
    }

    // (Tùy chọn) Hủy bỏ Worker khi không cần thiết
    private void cancelWeatherCheckWorker() {
        WorkManager.getInstance(this).cancelUniqueWork(WEATHER_CHECK_WORK_TAG);
        Log.i(TAG, "Cancelled WeatherCheckWorker.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");
        // Kiểm tra lại GPS khi người dùng quay lại ứng dụng (ví dụ: sau khi bật từ cài đặt)
        // Chỉ kiểm tra lại nếu đã có quyền vị trí
        if (isLocationPermissionGranted) {
            Log.d(TAG, "Checking GPS and location again onResume.");
            checkGpsAndGetLocation();
        } else {
            Log.d(TAG, "Skipping GPS check onResume as location permission is not granted.");
            // Có thể yêu cầu lại quyền ở đây nếu muốn người dùng cấp quyền khi quay lại app
            // requestLocationPermissionIfNeeded();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called.");
        // Cân nhắc việc hủy worker ở đây nếu bạn không muốn nó chạy sau khi activity bị hủy hoàn toàn
        // Tuy nhiên, thường thì worker nên tiếp tục chạy nền.
        // cancelWeatherCheckWorker();
        super.onDestroy();
    }
}

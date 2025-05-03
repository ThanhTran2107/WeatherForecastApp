package com.example.weatherforecastapp.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.util.Log; // Import Log
import androidx.annotation.NonNull; // Import NonNull
import androidx.annotation.Nullable; // Import Nullable
import com.example.weatherforecastapp.models.CurrentWeather; // Import CurrentWeather
import com.example.weatherforecastapp.utils.Constants; // Import Constants
import java.util.Locale; // Import Locale
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.models.LocationInfo;
import com.example.weatherforecastapp.viewmodel.WeatherViewModel;
import com.example.weatherforecastapp.utils.UnitConverter;

import java.io.Serializable;

import static com.example.weatherforecastapp.utils.Constants.API_KEY;
import static com.example.weatherforecastapp.utils.Constants.ARG_LOCATION;

public class WeatherFragment extends Fragment {
    private static final String TAG = "WeatherFragment";// Thêm TAG để ghi log
    private ImageView weatherIcon; // Thêm weatherIcon
    private WeatherViewModel viewModel;

    private TextView locationTextView, tempTextView, humidityTextView, conditionTextView,fahrenheitTextView;

    public static WeatherFragment newInstance(LocationInfo locationInfo) {
        WeatherFragment fragment = new WeatherFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.ARG_LOCATION, (Serializable) locationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        // Khởi tạo view
        weatherIcon = view.findViewById(R.id.weatherIcon); // Ánh xạ ImageView
        fahrenheitTextView = view.findViewById(R.id.fahrenheitTextView); // Ánh xạ TextView độ F
        locationTextView = view.findViewById(R.id.locationTextView); // Ánh xạ TextView vị trí
        tempTextView = view.findViewById(R.id.temperatureTextView); // Ánh xạ TextView độ C
        humidityTextView = view.findViewById(R.id.humidityTextView); // Ánh xạ TextView độ ẩm
        conditionTextView = view.findViewById(R.id.conditionTextView); // Ánh xạ TextView điều kiện

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        if (getArguments() != null) {
            LocationInfo locationInfo = (LocationInfo) getArguments().getSerializable(Constants.ARG_LOCATION);
            if (locationInfo != null) {
                double lat = locationInfo.getLatitude();
                double lon = locationInfo.getLongitude();
                Log.d(TAG, "Location received: Lat=" + lat + ", Lon=" + lon);

                // Hiển thị tạm "Đang tải..."
                // Nên dùng string resource: locationTextView.setText(R.string.loading_location);
                locationTextView.setText("Đang tải vị trí...");

                // Gọi ViewModel để lấy dữ liệu thời tiết
                observeWeatherData(lat, lon);

            } else {
                Log.w(TAG, "LocationInfo is null in arguments.");
                // Nên dùng string resource: locationTextView.setText(R.string.location_not_available);
                locationTextView.setText("Không có thông tin vị trí");
            }
        } else {
            Log.w(TAG, "Arguments are null.");
            // Nên dùng string resource: locationTextView.setText(R.string.location_not_available);
            locationTextView.setText("Không có thông tin vị trí");
        }
    }

    private void observeWeatherData(double lat, double lon) {
        // Quan sát LiveData từ ViewModel
        viewModel.getCurrentWeather(lat, lon, Constants.API_KEY).observe(getViewLifecycleOwner(), weather -> {
            if (weather != null) {
                Log.d(TAG, "Weather data received: City=" + weather.name);
                // Dữ liệu thời tiết đã được cập nhật, gọi hàm cập nhật UI
                updateLocationUI(weather); // Chỉ cập nhật vị trí
            } else {
                // Xử lý trường hợp viewModel trả về null (có thể là lỗi)
                Log.e(TAG, "Received null weather data from ViewModel.");
                // Hiển thị tọa độ nếu không lấy được tên
                if (locationTextView != null && locationTextView.getText().toString().contains("Đang tải")) {
                    locationTextView.setText(String.format(Locale.getDefault(),"Lat: %.4f, Lon: %.4f", lat, lon));
                }
            }
        });
    }

    // Hàm chỉ cập nhật TextView vị trí
    @SuppressLint("SetTextI18n")
    private void updateLocationUI(CurrentWeather weather) {
        if (locationTextView == null) return; // Kiểm tra null cho TextView

        if (weather.name != null && !weather.name.isEmpty()) {
            String displayLocation = weather.name;
            // (Tùy chọn) Thêm mã quốc gia nếu có và khác rỗng
            if (weather.sys != null && weather.sys.country != null && !weather.sys.country.isEmpty()) {
                displayLocation += ", " + weather.sys.country;
            }
            locationTextView.setText(displayLocation); // <<< Hiển thị tên thành phố
            Log.d(TAG, "Updating location text to: " + displayLocation);
        } else if (weather.coord != null) {
            // Nếu không có tên, hiển thị lại tọa độ từ dữ liệu trả về
            locationTextView.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", weather.coord.lat, weather.coord.lon));
            Log.w(TAG, "City name is null or empty, displaying coordinates.");
        } else {
            // Trường hợp không có cả tên và tọa độ từ API
            // Nên dùng string resource: locationTextView.setText(R.string.location_unknown);
            locationTextView.setText("Vị trí không xác định");
            Log.w(TAG, "City name and coordinates are null.");
        }
    }
}

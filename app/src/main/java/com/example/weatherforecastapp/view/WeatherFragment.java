package com.example.weatherforecastapp.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView; // Import ImageView

import androidx.annotation.NonNull; // Import NonNull
import androidx.annotation.Nullable; // Import Nullable
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.models.LocationInfo;
import com.example.weatherforecastapp.models.CurrentWeather; // Import CurrentWeather
import com.example.weatherforecastapp.viewmodel.WeatherViewModel;
import com.example.weatherforecastapp.utils.UnitConverter; // Import UnitConverter
import com.example.weatherforecastapp.utils.Constants; // Import Constants

import java.io.Serializable;
import java.util.Locale; // Import Locale

public class WeatherFragment extends Fragment {

    private static final String TAG = "WeatherFragment";
    private WeatherViewModel viewModel;

    // --- Khai báo các View ---
    private TextView locationTextView, tempTextView, humidityTextView, conditionTextView;

    private TextView fahrenheitTextView; // Đổi tên FTextView thành fahrenheitTextView
    private ImageView weatherIcon;
    // --------------------------

    public static WeatherFragment newInstance(LocationInfo locationInfo) {
        WeatherFragment fragment = new WeatherFragment();
        Bundle args = new Bundle();
        // Sử dụng Constants.ARG_LOCATION
        args.putSerializable(Constants.ARG_LOCATION, (Serializable) locationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"}) // Thêm DefaultLocale vào SuppressLint
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        // ---------------------------------
        weatherIcon = view.findViewById(R.id.weatherIcon);
        fahrenheitTextView = view.findViewById(R.id.fahrenheitTextView); // Sử dụng tên biến đã đổi
        locationTextView = view.findViewById(R.id.locationTextView);
        tempTextView = view.findViewById(R.id.temperatureTextView);
        humidityTextView = view.findViewById(R.id.humidityTextView);
        conditionTextView = view.findViewById(R.id.conditionTextView);
        // ---------------------------------

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);


        if (getArguments() != null) {
            // Sử dụng Constants.ARG_LOCATION
            LocationInfo locationInfo = (LocationInfo) getArguments().getSerializable(Constants.ARG_LOCATION);
            if (locationInfo != null) {
                double lat = locationInfo.getLatitude();
                double lon = locationInfo.getLongitude();
                Log.d(TAG, "Location received: Lat=" + lat + ", Lon=" + lon);

                // Hiển thị tạm "Đang tải..." hoặc tọa độ ban đầu (TÙY CHỌN)
                locationTextView.setText("Đang tải vị trí...");

                // Quan sát thời tiết hiện tại
                // Sử dụng Constants.API_KEY
                viewModel.getCurrentWeather(lat, lon, Constants.API_KEY).observe(getViewLifecycleOwner(), weather -> {
                    if (weather != null) {
                        Log.d(TAG, "Weather data received: City=" + weather.name);

                        // *** THAY ĐỔI Ở ĐÂY: Cập nhật locationTextView với tên thành phố ***
                        if (locationTextView != null) {
                            if (weather.name != null && !weather.name.isEmpty()) {
                                String displayLocation = weather.name;
                                if (weather.sys != null && weather.sys.country != null && !weather.sys.country.isEmpty()) {
                                    displayLocation += ", " + weather.sys.country;
                                }
                                locationTextView.setText(displayLocation);
                                Log.d(TAG, "Updating location text to: " + displayLocation);
                            } else if (weather.coord != null) {
                                // Nếu không có tên, hiển thị lại tọa độ từ dữ liệu trả về
                                locationTextView.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", weather.coord.lat, weather.coord.lon));
                                Log.w(TAG, "City name is null or empty, displaying coordinates.");
                            } else {
                                locationTextView.setText("Vị trí không xác định");
                                Log.w(TAG, "City name and coordinates are null.");
                            }
                        }
                        // ***************************************************************

                        if (weather.main != null) {
                            if (tempTextView != null) {
                                tempTextView.setText("Temp: " + weather.main.temp + "°C");
                            }
                            if (fahrenheitTextView != null) {
                                try {
                                    double tempCelsius = weather.main.temp;
                                    double tempFahrenheit = UnitConverter.celsiusToFahrenheit(tempCelsius);
                                    // Sử dụng tên biến fahrenheitTextView
                                    fahrenheitTextView.setText(String.format("%.1f", tempFahrenheit) + "°F");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting temperature", e);
                                    fahrenheitTextView.setText("");
                                }
                            }
                            if (humidityTextView != null) {
                                humidityTextView.setText("Humidity: " + weather.main.humidity + "%");
                            }
                        } else {
                            Log.w(TAG, "Weather 'main' data is null.");
                            // Có thể đặt giá trị mặc định cho các TextView ở đây
                        }

                        if (weather.weather != null && !weather.weather.isEmpty()) {
                            CurrentWeather.Weather currentCondition = weather.weather.get(0);
                            if (conditionTextView != null) {
                                conditionTextView.setText("Condition: " + (currentCondition.description != null ? currentCondition.description : "--"));
                            }


                            if (weatherIcon != null && currentCondition.description != null) {
                                String lowerDesc = currentCondition.description.toLowerCase(Locale.ROOT);
                                if (lowerDesc.contains("rain")) {
                                    weatherIcon.setImageResource(R.drawable.ic_rain);
                                } else if (lowerDesc.contains("sun") || lowerDesc.contains("clear")) { // Thêm clear
                                    weatherIcon.setImageResource(R.drawable.ic_sun);
                                } else if (lowerDesc.contains("cloud")) {
                                    weatherIcon.setImageResource(R.drawable.ic_cloud);
                                }
                                // Thêm các điều kiện khác nếu cần (snow, thunderstorm, etc.)
                                else {
                                    weatherIcon.setImageResource(R.drawable.ic_cloud); // Icon mặc định
                                }
                                Log.d(TAG, "Setting weather icon based on description: " + currentCondition.description);
                            }
                        } else {
                            Log.w(TAG, "Weather 'weather' data is null or empty.");
                            if (conditionTextView != null) conditionTextView.setText("Condition: --");
                            if (weatherIcon != null) weatherIcon.setImageResource(R.drawable.ic_cloud);
                        }
                        // -----------------------------------------------------------------
                    } else {
                        Log.e(TAG, "Received null weather data from ViewModel.");
                        // Xử lý khi không nhận được dữ liệu thời tiết
                        if (locationTextView != null && locationTextView.getText().toString().contains("Đang tải")) {
                            // Hiển thị lại tọa độ ban đầu nếu vẫn đang tải
                            locationTextView.setText(String.format(Locale.getDefault(),"Lat: %.4f, Lon: %.4f", lat, lon));
                        }
                    }
                });
            } else {
                Log.w(TAG, "LocationInfo is null in arguments.");
                locationTextView.setText("Không có thông tin vị trí");
            }
        } else {
            Log.w(TAG, "Arguments are null.");
            locationTextView.setText("Không có thông tin vị trí");
        }
        // -------------------------------------------------

        return view;
    }

}

package com.example.weatherforecastapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.weatherforecastapp.models.CurrentWeather;

import com.example.weatherforecastapp.repository.WeatherRepository;

import java.util.List;

public class WeatherViewModel extends ViewModel {

    private final WeatherRepository repository = new WeatherRepository();

    // ✅ Thời tiết hiện tại
    public LiveData<CurrentWeather> getCurrentWeather(double lat, double lon, String apiKey) {
        return repository.getCurrentWeather(lat, lon, apiKey);
    }

    // Thêm thời tiết theo ngày
    // Thêm thời tiết theo giờ
}

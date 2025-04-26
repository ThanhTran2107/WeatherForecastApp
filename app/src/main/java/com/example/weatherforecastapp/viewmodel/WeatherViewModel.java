package com.example.weatherforecastapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.weatherforecastapp.models.CurrentWeather;
import com.example.weatherforecastapp.models.DailyForecast;
import com.example.weatherforecastapp.models.ForecastResponse;
import com.example.weatherforecastapp.repository.WeatherRepository;

import java.util.List;

public class WeatherViewModel extends ViewModel {

    private final WeatherRepository repository = new WeatherRepository();

    // ✅ Thời tiết hiện tại
    public LiveData<CurrentWeather> getCurrentWeather(double lat, double lon, String apiKey) {
        return repository.getCurrentWeather(lat, lon, apiKey);
    }

    // ✅ Thời tiết theo giờ
    public LiveData<ForecastResponse> getHourlyForecast(double lat, double lon, String apiKey, String units) {
        return repository.getHourlyForecast(lat, lon, apiKey, units);
    }

    // ✅ Thời tiết theo ngày
    public LiveData<List<DailyForecast>> getDailyForecast(double lat, double lon, String apiKey, String units) {
        return repository.getDailyForecast(lat, lon, apiKey, units);
    }
}
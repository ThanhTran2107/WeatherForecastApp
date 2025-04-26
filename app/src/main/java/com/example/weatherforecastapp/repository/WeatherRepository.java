package com.example.weatherforecastapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.weatherforecastapp.models.CurrentWeather;
import com.example.weatherforecastapp.models.DailyForecast;
import com.example.weatherforecastapp.models.ForecastResponse;
import com.example.weatherforecastapp.network.RetrofitClient;
import com.example.weatherforecastapp.network.WeatherApiService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {

    private final WeatherApiService apiService;

    public WeatherRepository() {
        apiService = RetrofitClient.getApiService();
    }

    // Dự báo hiện tại
    public LiveData<CurrentWeather> getCurrentWeather(double lat, double lon, String apiKey) {
        MutableLiveData<CurrentWeather> data = new MutableLiveData<>();
        apiService.getCurrentWeather(lat, lon, apiKey, "metric")
                .enqueue(new Callback<CurrentWeather>() {
                    @Override
                    public void onResponse(Call<CurrentWeather> call, Response<CurrentWeather> response) {
                        if (response.isSuccessful()) {
                            data.setValue(response.body());
                        } else {
                            data.setValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<CurrentWeather> call, Throwable t) {
                        data.setValue(null);
                    }
                });
        return data;
    }

    // Dự báo theo giờ
    public LiveData<ForecastResponse> getHourlyForecast(double lat, double lon, String apiKey, String units) {
        MutableLiveData<ForecastResponse> data = new MutableLiveData<>();
        apiService.getHourlyForecast(lat, lon, apiKey, units)
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                        if (response.isSuccessful()) {
                            data.setValue(response.body());
                        } else {
                            data.setValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable t) {
                        data.setValue(null);
                    }
                });
        return data;
    }

    // Dự báo theo ngày
    public LiveData<List<DailyForecast>> getDailyForecast(double lat, double lon, String apiKey, String units) {
        MutableLiveData<List<DailyForecast>> data = new MutableLiveData<>();
        apiService.getHourlyForecast(lat, lon, apiKey, units)
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().list != null) {
                            // Sử dụng TreeMap để đảm bảo thứ tự ngày
                            Map<String, List<ForecastResponse.ForecastItem>> dailyGroups = new TreeMap<>();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

                            // Nhóm dữ liệu theo ngày
                            for (ForecastResponse.ForecastItem item : response.body().list) {
                                String date = dateFormat.format(new Date(item.dt * 1000L));
                                dailyGroups.computeIfAbsent(date, k -> new ArrayList<>()).add(item);
                            }

                            List<DailyForecast> dailyForecasts = new ArrayList<>();
                            int dayCount = 0;
                            for (String date : dailyGroups.keySet()) {
                                if (dayCount >= 5) break;
                                List<ForecastResponse.ForecastItem> items = dailyGroups.get(date);
                                double maxTemp = items.stream().mapToDouble(i -> i.main.temp).max().orElse(0.0);
                                double minTemp = items.stream().mapToDouble(i -> i.main.temp).min().orElse(0.0);
                                try {
                                    String displayDate = displayFormat.format(dateFormat.parse(date));
                                    dailyForecasts.add(new DailyForecast(displayDate, minTemp, maxTemp));
                                    dayCount++;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            data.setValue(dailyForecasts);
                        } else {
                            data.setValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable t) {
                        data.setValue(null);
                    }
                });
        return data;
    }
}
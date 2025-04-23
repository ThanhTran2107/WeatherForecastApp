package com.example.weatherforecastapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.weatherforecastapp.models.CurrentWeather;
import com.example.weatherforecastapp.network.RetrofitClient;
import com.example.weatherforecastapp.network.WeatherApiService;



import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {

    private final WeatherApiService apiService;

    public WeatherRepository() {
        apiService = RetrofitClient.getApiService();
    }

    // ✅ Dự báo hiện tại
    public LiveData<CurrentWeather> getCurrentWeather(double lat, double lon, String apiKey) {
        MutableLiveData<CurrentWeather> data = new MutableLiveData<>();

        apiService.getCurrentWeather(lat, lon, apiKey, "metric")
                .enqueue(new Callback<>() {
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

    // Thêm dự báo theo ngày
    // Thêm dự báo theo giờ
}

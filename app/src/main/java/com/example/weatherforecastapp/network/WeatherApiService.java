package com.example.weatherforecastapp.network;

import com.example.weatherforecastapp.models.CurrentWeather;
import com.example.weatherforecastapp.models.ForecastResponse;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    @GET("weather")
    Call<CurrentWeather> getCurrentWeather(@Query("lat") double lat, @Query("lon") double lon,
                                           @Query("appid") String apiKey,
                                           @Query("units") String units);

    @GET("forecast")
    Call<ForecastResponse> getHourlyForecast(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units
    );
}

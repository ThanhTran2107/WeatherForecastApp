package com.example.weatherforecastapp.network;

import com.example.weatherforecastapp.models.CurrentWeather;
import com.example.weatherforecastapp.models.DailyForecast;
import com.example.weatherforecastapp.models.HourlyForecast;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    @GET("weather")
    Call<CurrentWeather> getCurrentWeather(@Query("lat") double lat, @Query("lon") double lon,
                                           @Query("appid") String apiKey,
                                           @Query("units") String units);

    @GET("forecast/hourly")
    Call<List<HourlyForecast>> getHourlyForecast(@Query("lat") double lat, @Query("lon") double lon,
                                                 @Query("appid") String apiKey);

    @GET("forecast/daily")
    Call<List<DailyForecast>> getDailyForecast(@Query("lat") double lat, @Query("lon") double lon,
                                               @Query("appid") String apiKey);

}

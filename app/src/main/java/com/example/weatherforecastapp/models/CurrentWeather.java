package com.example.weatherforecastapp.models;

import java.util.List;

public class CurrentWeather {
    public Main main;
    public List<Weather> weather;

    public static class Main {
        public float temp;
        public int humidity;
    }

    public static class Weather {
        public String description;
    }
}

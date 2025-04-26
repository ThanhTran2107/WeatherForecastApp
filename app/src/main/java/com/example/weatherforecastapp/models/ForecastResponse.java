package com.example.weatherforecastapp.models;

import java.util.List;

public class ForecastResponse {
    public List<ForecastItem> list;

    public static class ForecastItem {
        public long dt;
        public Main main;
        public List<Weather> weather;

        public static class Main {
            public double temp;
        }

        public static class Weather {
            public String icon;
        }
    }
}
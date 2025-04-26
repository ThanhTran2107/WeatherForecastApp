package com.example.weatherforecastapp.models;

import java.util.List;

public class DailyForecastResponse {
    public List<DailyItem> daily;

    public static class DailyItem {
        public long dt;
        public Temp temp;
        public List<Weather> weather;

        public static class Temp {
            public double max;
            public double min;
        }

        public static class Weather {
            public String icon;
        }
    }
}
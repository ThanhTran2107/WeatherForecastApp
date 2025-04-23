package com.example.weatherforecastapp.models;

public class HourlyForecast {
    private String time;
    private double temperature;

    public HourlyForecast(String time, double temperature) {
        this.time = time;
        this.temperature = temperature;
    }

    public String getTime() {
        return time;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}

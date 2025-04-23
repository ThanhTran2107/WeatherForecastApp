package com.example.weatherforecastapp.models;

public class DailyForecast {
    private String date;
    private double minTemp;
    private double maxTemp;

    public DailyForecast(String date, double minTemp, double maxTemp) {
        this.date = date;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
    }

    public String getDate() {
        return date;
    }

    public double getMinTemp() {
        return minTemp;
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setMinTemp(double minTemp) {
        this.minTemp = minTemp;
    }

    public void setMaxTemp(double maxTemp) {
        this.maxTemp = maxTemp;
    }
}

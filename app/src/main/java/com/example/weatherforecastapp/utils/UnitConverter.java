package com.example.weatherforecastapp.utils;

public class UnitConverter {

    // Chuyển Celsius sang Fahrenheit
    public static double celsiusToFahrenheit(double celsius) {
        return (celsius * 9/5) + 32;
    }

    // Chuyển Fahrenheit sang Celsius (nếu cần)
    public static double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32) * 5/9;
    }
}

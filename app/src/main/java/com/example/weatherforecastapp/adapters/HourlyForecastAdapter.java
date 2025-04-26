package com.example.weatherforecastapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.models.HourlyForecast;

import java.util.List;

public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.ViewHolder> {

    private List<HourlyForecast> hourlyForecasts;

    public HourlyForecastAdapter(List<HourlyForecast> hourlyForecasts) {
        this.hourlyForecasts = hourlyForecasts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HourlyForecast forecast = hourlyForecasts.get(position);
        holder.hourTextView.setText(forecast.getTime());
        holder.hourTempTextView.setText(String.format("%d°C", (int) forecast.getTemperature()));
    }

    @Override
    public int getItemCount() {
        return hourlyForecasts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView hourTextView;
        TextView hourTempTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            hourTextView = itemView.findViewById(R.id.hourTextView);
            hourTempTextView = itemView.findViewById(R.id.hourTempTextView);
        }
    }
}
package com.example.weatherforecastapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.models.DailyForecast;

import java.util.List;

public class DailyForecastAdapter extends RecyclerView.Adapter<DailyForecastAdapter.ViewHolder> {

    private List<DailyForecast> dailyForecasts;

    public DailyForecastAdapter(List<DailyForecast> dailyForecasts) {
        this.dailyForecasts = dailyForecasts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyForecast forecast = dailyForecasts.get(position);
        holder.dateTextView.setText(forecast.getDate());
        holder.dailyTempTextView.setText(String.format("Min: %d°C | Max: %d°C",
                (int) forecast.getMinTemp(), (int) forecast.getMaxTemp()));
    }

    @Override
    public int getItemCount() {
        return dailyForecasts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;
        TextView dailyTempTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            dailyTempTextView = itemView.findViewById(R.id.dailyTempTextView);
        }
    }
}
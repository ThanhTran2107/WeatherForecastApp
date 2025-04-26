package com.example.weatherforecastapp.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.adapters.DailyForecastAdapter;
import com.example.weatherforecastapp.adapters.HourlyForecastAdapter;
import com.example.weatherforecastapp.models.ForecastResponse;
import com.example.weatherforecastapp.models.HourlyForecast;
import com.example.weatherforecastapp.models.LocationInfo;
import com.example.weatherforecastapp.viewmodel.WeatherViewModel;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.example.weatherforecastapp.utils.Constants.API_KEY;
import static com.example.weatherforecastapp.utils.Constants.ARG_LOCATION;
import static com.example.weatherforecastapp.utils.Constants.UNIT_METRIC;

public class ForecastFragment extends Fragment {

    private WeatherViewModel viewModel;
    private RecyclerView hourlyRecyclerView;
    private RecyclerView dailyRecyclerView;
    private HourlyForecastAdapter hourlyAdapter;
    private DailyForecastAdapter dailyAdapter;

    public static ForecastFragment newInstance(LocationInfo locationInfo) {
        ForecastFragment fragment = new ForecastFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LOCATION, (Serializable) locationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forecast, container, false);

        hourlyRecyclerView = view.findViewById(R.id.rvHourlyForecast);
        dailyRecyclerView = view.findViewById(R.id.rvDailyForecast);

        hourlyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        hourlyAdapter = new HourlyForecastAdapter(new ArrayList<>());
        hourlyRecyclerView.setAdapter(hourlyAdapter);

        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dailyAdapter = new DailyForecastAdapter(new ArrayList<>());
        dailyRecyclerView.setAdapter(dailyAdapter);

        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        if (getArguments() != null) {
            LocationInfo locationInfo = (LocationInfo) getArguments().getSerializable(ARG_LOCATION);
            if (locationInfo != null) {
                double lat = locationInfo.getLatitude();
                double lon = locationInfo.getLongitude();
                fetchForecastData(lat, lon);
            }
        }

        return view;
    }

    private void fetchForecastData(double lat, double lon) {
        // Du bao theo gio
        viewModel.getHourlyForecast(lat, lon, API_KEY, UNIT_METRIC).observe(getViewLifecycleOwner(), forecastResponse -> {
            if (forecastResponse != null && forecastResponse.list != null) {
                List<HourlyForecast> hourlyForecasts = new ArrayList<>();
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                for (int i = 0; i < Math.min(24, forecastResponse.list.size()); i++) {
                    ForecastResponse.ForecastItem item = forecastResponse.list.get(i);
                    String time = timeFormat.format(new Date(item.dt * 1000L));
                    double temp = item.main.temp;
                    hourlyForecasts.add(new HourlyForecast(time, temp));
                }

                hourlyAdapter = new HourlyForecastAdapter(hourlyForecasts);
                hourlyRecyclerView.setAdapter(hourlyAdapter);
            } else {
                Toast.makeText(getContext(), "Lỗi khi lấy dự báo theo giờ", Toast.LENGTH_SHORT).show();
            }
        });

        // Du bao theo ngay
        viewModel.getDailyForecast(lat, lon, API_KEY, UNIT_METRIC).observe(getViewLifecycleOwner(), dailyForecasts -> {
            if (dailyForecasts != null && !dailyForecasts.isEmpty()) {
                dailyAdapter = new DailyForecastAdapter(dailyForecasts);
                dailyRecyclerView.setAdapter(dailyAdapter);
            } else {
                Toast.makeText(getContext(), "Lỗi khi lấy dự báo theo ngày", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
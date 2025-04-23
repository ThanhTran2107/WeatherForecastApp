package com.example.weatherforecastapp.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.models.LocationInfo;
import com.example.weatherforecastapp.viewmodel.WeatherViewModel;

import java.io.Serializable;

import static com.example.weatherforecastapp.utils.Constants.API_KEY;
import static com.example.weatherforecastapp.utils.Constants.ARG_LOCATION;

public class WeatherFragment extends Fragment {

    private WeatherViewModel viewModel;

    private TextView locationTextView, tempTextView, humidityTextView, conditionTextView;

    public static WeatherFragment newInstance(LocationInfo locationInfo) {
        WeatherFragment fragment = new WeatherFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LOCATION, (Serializable) locationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        // Khởi tạo view
        locationTextView = view.findViewById(R.id.locationTextView);
        tempTextView = view.findViewById(R.id.temperatureTextView);
        humidityTextView = view.findViewById(R.id.humidityTextView);
        conditionTextView = view.findViewById(R.id.conditionTextView);

        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        if (getArguments() != null) {
            LocationInfo locationInfo = (LocationInfo) getArguments().getSerializable(ARG_LOCATION);
            if (locationInfo != null) {
                double lat = locationInfo.getLatitude();
                double lon = locationInfo.getLongitude();

                locationTextView.setText("Lat: " + lat + ", Lon: " + lon);

                // Quan sát thời tiết hiện tại
                viewModel.getCurrentWeather(lat, lon, API_KEY).observe(getViewLifecycleOwner(), weather -> {
                    if (weather != null) {
                        tempTextView.setText("Temp: " + weather.main.temp + "°C");
                        humidityTextView.setText("Humidity: " + weather.main.humidity + "%");
                        conditionTextView.setText("Condition: " + weather.weather.get(0).description);
                    }
                });
            }
        }

        return view;
    }
}

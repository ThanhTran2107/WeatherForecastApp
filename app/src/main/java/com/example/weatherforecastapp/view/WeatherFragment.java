package com.example.weatherforecastapp.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.models.LocationInfo;

import java.io.Serializable;

public class WeatherFragment extends Fragment {

    private static final String ARG_LOCATION = "location_info";

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
        View rootView = inflater.inflate(R.layout.fragment_weather, container, false);

        TextView locationTextView = rootView.findViewById(R.id.locationTextView);
        if (getArguments() != null) {
            LocationInfo locationInfo = (LocationInfo) getArguments().getSerializable(ARG_LOCATION);
            if (locationInfo != null) {
                locationTextView.setText("Latitude: " + locationInfo.getLatitude() + "\nLongitude: " + locationInfo.getLongitude());
            }
        }

        return rootView;
    }
}

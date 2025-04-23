package com.example.weatherforecastapp.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.models.LocationInfo;

public class MapFragment extends Fragment {
    private static final String ARG_LOCATION = "location_info";

    public static MapFragment newInstance(LocationInfo locationInfo) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LOCATION, locationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        if (getArguments() != null) {
            LocationInfo locationInfo = (LocationInfo) getArguments().getSerializable(ARG_LOCATION);
            if (locationInfo != null) {
                double lat = locationInfo.getLatitude();
                double lon = locationInfo.getLongitude();
                // TODO: Hiển thị vị trí này trên Google Map hoặc Static Map
            }
        }

        return view;
    }
}

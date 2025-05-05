package com.example.weatherforecastapp.view;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.utils.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.android.material.button.MaterialButton;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TileOverlay tempOverlay;
    private TileOverlay rainOverlay;
    private TileOverlay cloudOverlay;
    private Button btnToggleTemp;
    private Button btnToggleRain;
    private Button btnToggleCloud;
    private static final String TAG = "MapFragment";
    private static final LatLng DEFAULT_VIETNAM_LOCATION = new LatLng(21.0285, 105.8542);
    private static final float DEFAULT_VIETNAM_ZOOM = 6f;
    private static final float USER_LOCATION_ZOOM = 10f;
    private static final float OVERLAY_TRANSPARENCY = 0.0f;
    private static final String OWM_TILE_URL_FORMAT =
            "https://tile.openweathermap.org/map/%s/%d/%d/%d.png?appid=%s";
    private static final String ARG_LAT = "target_lat";
    private static final String ARG_LON = "target_lon";
    private LatLng targetLatLng;

    public static MapFragment newInstance(double targetLat, double targetLon) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, targetLat);
        args.putDouble(ARG_LON, targetLon);
        fragment.setArguments(args);
        Log.d(TAG, "newInstance created with target: " + targetLat + ", " + targetLon);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        targetLatLng = null;
        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_LAT) && getArguments().containsKey(ARG_LON)) {
                double lat = getArguments().getDouble(ARG_LAT);
                double lon = getArguments().getDouble(ARG_LON);
                targetLatLng = new LatLng(lat, lon);
                Log.d(TAG, "Target coordinates received in onCreate: " + targetLatLng);
            } else {
                Log.w(TAG, "Target coordinates (ARG_LAT, ARG_LON) not found in arguments.");
            }
        } else {
            Log.w(TAG, "Arguments are null in onCreate");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "SupportMapFragment with ID 'map' not found!");
            if(getContext() != null) Toast.makeText(getContext(), "Lỗi khi tải bản đồ", Toast.LENGTH_LONG).show();
        }

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                FragmentManager fm = getParentFragmentManager();
                if (fm.getBackStackEntryCount() > 0) fm.popBackStack();
            });
        } else { Log.e(TAG, "Back button (btnBack) not found!"); }

        btnToggleTemp = view.findViewById(R.id.btnToggleTemp);
        btnToggleRain = view.findViewById(R.id.btnToggleRain);
        btnToggleCloud = view.findViewById(R.id.btnToggleCloud);

        if (btnToggleTemp != null) btnToggleTemp.setOnClickListener(v -> toggleOverlay(tempOverlay, btnToggleTemp)); else Log.w(TAG, "btnToggleTemp not found");
        if (btnToggleRain != null) btnToggleRain.setOnClickListener(v -> toggleOverlay(rainOverlay, btnToggleRain)); else Log.w(TAG, "btnToggleRain not found");
        if (btnToggleCloud != null) btnToggleCloud.setOnClickListener(v -> toggleOverlay(cloudOverlay, btnToggleCloud)); else Log.w(TAG, "btnToggleCloud not found");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.i(TAG, "Map is ready.");
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);
        uiSettings.setRotateGesturesEnabled(true);

        if (targetLatLng != null) {
            Log.d(TAG, "Displaying target location: " + targetLatLng);
            mMap.addMarker(new MarkerOptions().position(targetLatLng).title("Vị trí tìm kiếm"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, USER_LOCATION_ZOOM));
        } else {
            Log.w(TAG, "Target LatLng is null. Displaying default Vietnam location.");
            if(getContext() != null) Toast.makeText(getContext(), "Không có vị trí, hiển thị bản đồ mặc định", Toast.LENGTH_SHORT).show();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_VIETNAM_LOCATION, DEFAULT_VIETNAM_ZOOM));
        }

        addWeatherOverlays();

        updateButtonState(tempOverlay, btnToggleTemp);
        updateButtonState(rainOverlay, btnToggleRain);
        updateButtonState(cloudOverlay, btnToggleCloud);
    }

    private void addWeatherOverlays() {
        if (mMap == null) { Log.w(TAG, "Cannot add overlays, map is null."); return; }
        Log.d(TAG, "Adding weather overlays...");
        TileProvider tempTileProvider = createTileProvider("temp_new");
        if (tempTileProvider != null) {
            tempOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tempTileProvider).fadeIn(true).transparency(OVERLAY_TRANSPARENCY).zIndex(1f));
            if(tempOverlay != null) { tempOverlay.setVisible(true); Log.d(TAG, "Temperature overlay added."); } else { Log.e(TAG, "Failed to add temperature overlay."); }
        } else { Log.e(TAG, "Failed to create temperature tile provider."); }
        TileProvider rainTileProvider = createTileProvider("precipitation_new");
        if (rainTileProvider != null) {
            rainOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(rainTileProvider).fadeIn(true).transparency(OVERLAY_TRANSPARENCY).zIndex(2f));
            if(rainOverlay != null) { rainOverlay.setVisible(false); Log.d(TAG, "Precipitation overlay added."); } else { Log.e(TAG, "Failed to add precipitation overlay."); }
        } else { Log.e(TAG, "Failed to create precipitation tile provider."); }
        TileProvider cloudTileProvider = createTileProvider("clouds_new");
        if (cloudTileProvider != null) {
            cloudOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(cloudTileProvider).fadeIn(true).transparency(OVERLAY_TRANSPARENCY).zIndex(3f));
            if(cloudOverlay != null) { cloudOverlay.setVisible(false); Log.d(TAG, "Cloud overlay added."); } else { Log.e(TAG, "Failed to add cloud overlay."); }
        } else { Log.e(TAG, "Failed to create cloud tile provider."); }
    }

    private void toggleOverlay(TileOverlay overlay, Button button) {
        if (overlay != null && button != null) {
            boolean isVisible = !overlay.isVisible();
            overlay.setVisible(isVisible);
            updateButtonState(overlay, button);
            Log.d(TAG, "Overlay visibility toggled to: " + isVisible);
        } else { Log.w(TAG, "Cannot toggle overlay or button is null."); }
    }

    private void updateButtonState(TileOverlay overlay, Button button) {
        if (overlay != null && button != null && getContext() != null) {
            boolean isVisible = overlay.isVisible();
            // Chỉnh lại màu sắc theo style bạn muốn
            int activeColor = R.color.black; // Màu khi active (ví dụ)
            int inactiveColor = R.color.black; // Màu khi inactive (ví dụ)
            int activeTextColor = Color.WHITE;
            int inactiveTextColor = Color.BLACK;

            if (isVisible) {
                if (button instanceof MaterialButton) { ((MaterialButton) button).setBackgroundTintList(ContextCompat.getColorStateList(getContext(), activeColor)); }
                else { button.setBackgroundColor(ContextCompat.getColor(getContext(), activeColor)); }
                button.setTextColor(activeTextColor);
            } else {
                if (button instanceof MaterialButton) { ((MaterialButton) button).setBackgroundTintList(ContextCompat.getColorStateList(getContext(), inactiveColor)); }
                else { button.setBackgroundColor(ContextCompat.getColor(getContext(), inactiveColor)); }
                button.setTextColor(inactiveTextColor);
            }
        } else { Log.w(TAG, "Cannot update button state, view or context is null."); }
    }

    private TileProvider createTileProvider(final String layer) {
        try {
            return new UrlTileProvider(256, 256) {
                @Nullable @Override public URL getTileUrl(int x, int y, int zoom) {
                    if (Constants.API_KEY == null || Constants.API_KEY.isEmpty() || Constants.API_KEY.equals("YOUR_API_KEY")) { Log.e("TileProvider", "API Key missing/invalid"); return null; }
                    String urlStr = String.format(Locale.US, OWM_TILE_URL_FORMAT, layer, zoom, x, y, Constants.API_KEY);
                    try { return new URL(urlStr); } catch (MalformedURLException e) { Log.e("TileProvider", "Malformed URL: " + urlStr, e); return null; }
                }
            };
        } catch (Exception e) { Log.e(TAG, "Error creating TileProvider for " + layer, e); return null; }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called, removing overlays.");
        if (tempOverlay != null) { tempOverlay.remove(); tempOverlay = null; }
        if (rainOverlay != null) { rainOverlay.remove(); rainOverlay = null; }
        if (cloudOverlay != null) { cloudOverlay.remove(); cloudOverlay = null; }
        mMap = null;
        super.onDestroyView();
    }
}
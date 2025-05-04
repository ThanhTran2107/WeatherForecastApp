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
// import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.models.LocationInfo;
import com.example.weatherforecastapp.utils.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment; // Đảm bảo import đúng
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
    private LocationInfo locationInfo;
    private TileOverlay tempOverlay;
    private TileOverlay rainOverlay;
    private TileOverlay cloudOverlay;
    // --- Các nút điều khiển ---
    private Button btnToggleTemp;
    private Button btnToggleRain;
    private Button btnToggleCloud;
    // -----------------------

    private static final String TAG = "MapFragment";
    // Tọa độ mặc định ở Việt Nam (ví dụ: Hà Nội)
    private static final LatLng DEFAULT_VIETNAM_LOCATION = new LatLng(21.0285, 105.8542);
    private static final float DEFAULT_VIETNAM_ZOOM = 6f; // Mức zoom để thấy tổng quan VN
    private static final float USER_LOCATION_ZOOM = 10f; // Mức zoom khi có vị trí người dùng

    private static final float OVERLAY_TRANSPARENCY = 0.0f;
    private static final String OWM_TILE_URL_FORMAT =
            "https://tile.openweathermap.org/map/%s/%d/%d/%d.png?appid=%s";

    public static MapFragment newInstance(LocationInfo locationInfo) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.ARG_LOCATION, locationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            locationInfo = (LocationInfo) getArguments().getSerializable(Constants.ARG_LOCATION);
            if (locationInfo == null) {
                Log.w(TAG, "LocationInfo is null in onCreate");
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
        // Lấy SupportMapFragment và đăng ký callback onMapReady
        // *** SỬA LỖI Ở ĐÂY: Sử dụng R.id.map ***
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map); // Sử dụng ID từ XML của bạn
        if (mapFragment != null) {
            Log.d(TAG, "Getting map async...");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "SupportMapFragment with ID 'map' not found!"); // Cập nhật log lỗi
            Toast.makeText(getContext(), "Lỗi khi tải bản đồ (không tìm thấy container)", Toast.LENGTH_LONG).show();
        }
        // --- Xử lý nút Back ---
        ImageButton btnBack = view.findViewById(R.id.btnBack); // Tìm nút bằng ID
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> { // Đặt hành động khi nhấn nút
                // Sử dụng FragmentManager để quay lại fragment trước đó
                FragmentManager fm = getParentFragmentManager();
                if (fm.getBackStackEntryCount() > 0) { // Kiểm tra xem có gì để back không
                    Log.d(TAG, "Navigating back.");
                    fm.popBackStack(); // Thực hiện hành động quay lại
                } else {
                    Log.w(TAG, "Back stack is empty, cannot navigate back.");
                    // Xử lý trường hợp không có gì để quay lại (ví dụ: đóng Activity)
                }
            });
        } else {
            Log.e(TAG, "Back button (btnBack) not found in layout!");
        }
        // --- Ánh xạ và xử lý nút điều khiển Overlay ---
        btnToggleTemp = view.findViewById(R.id.btnToggleTemp);
        btnToggleRain = view.findViewById(R.id.btnToggleRain);
        btnToggleCloud = view.findViewById(R.id.btnToggleCloud);

        if (btnToggleTemp != null) {
            btnToggleTemp.setOnClickListener(v -> toggleOverlay(tempOverlay, btnToggleTemp));
        }
        if (btnToggleRain != null) {
            btnToggleRain.setOnClickListener(v -> toggleOverlay(rainOverlay, btnToggleRain));
        }
        if (btnToggleCloud != null) {
            btnToggleCloud.setOnClickListener(v -> toggleOverlay(cloudOverlay, btnToggleCloud));
        }
        // --------------------------------------------
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.i(TAG, "Map is ready.");
// --- Cấu hình UI Settings ---
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true); // Hiển thị nút +/- Zoom
        uiSettings.setZoomGesturesEnabled(true); // Cho phép zoom bằng cử chỉ chụm/mở
        uiSettings.setScrollGesturesEnabled(true); // Cho phép cuộn bản đồ
        uiSettings.setRotateGesturesEnabled(true); // Cho phép xoay bản đồ
        uiSettings.setCompassEnabled(true); // Hiển thị la bàn khi xoay
        // --------------------------

        // Cập nhật trạng thái ban đầu của các nút điều khiển
        updateButtonState(tempOverlay, btnToggleTemp);
        updateButtonState(rainOverlay, btnToggleRain);
        updateButtonState(cloudOverlay, btnToggleCloud);
        // Kiểm tra xem có thông tin vị trí người dùng không
        if (locationInfo != null) {
            // Có vị trí người dùng -> Hiển thị vị trí đó
            LatLng currentPosition = new LatLng(locationInfo.getLatitude(), locationInfo.getLongitude());
            Log.d(TAG, "Displaying user location: " + currentPosition.latitude + ", " + currentPosition.longitude);

            mMap.addMarker(new MarkerOptions()
                    .position(currentPosition)
                    .title("Vị trí của bạn"));

            // Di chuyển camera đến vị trí người dùng với mức zoom phù hợp
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, USER_LOCATION_ZOOM));

            // Thêm lớp phủ thời tiết (chỉ thêm khi có vị trí cụ thể?)
            // Hoặc bạn có thể thêm lớp phủ ngay cả khi dùng vị trí mặc định
            addWeatherOverlays();

        } else {
            // Không có vị trí người dùng -> Hiển thị vị trí mặc định ở Việt Nam
            Log.w(TAG, "LocationInfo is null. Displaying default Vietnam location.");
            Toast.makeText(getContext(), "Không lấy được vị trí, hiển thị bản đồ Việt Nam", Toast.LENGTH_SHORT).show();

            // Di chuyển camera đến vị trí mặc định ở Việt Nam
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_VIETNAM_LOCATION, DEFAULT_VIETNAM_ZOOM));

            // Vẫn thêm lớp phủ thời tiết cho khu vực mặc định nếu muốn
            addWeatherOverlays();
        }
    }

    private void addWeatherOverlays() {
        if (mMap == null) {
            Log.w(TAG, "Cannot add overlays, map is null.");
            return;
        }
        Log.d(TAG, "Adding weather overlays...");

        TileProvider tempTileProvider = createTileProvider("temp_new");
        if (tempTileProvider != null) {
            tempOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                    .tileProvider(tempTileProvider)
                    .fadeIn(true)
                    .transparency(OVERLAY_TRANSPARENCY) // *** Đặt độ trong suốt ***
                    .zIndex(1f));
            tempOverlay.setVisible(true);
            Log.d(TAG, "Temperature overlay added.");
        } else {
            Log.e(TAG, "Failed to create temperature tile provider.");
        }

        TileProvider rainTileProvider = createTileProvider("precipitation_new");
        if (rainTileProvider != null) {
            rainOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                    .tileProvider(rainTileProvider)
                    .fadeIn(true)
                    .transparency(OVERLAY_TRANSPARENCY) // *** Đặt độ trong suốt ***
                    .zIndex(2f));
            rainOverlay.setVisible(false);
            Log.d(TAG, "Precipitation overlay added.");
        } else {
            Log.e(TAG, "Failed to create precipitation tile provider.");
        }

        TileProvider cloudTileProvider = createTileProvider("clouds_new");
        if (cloudTileProvider != null) {
            cloudOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                    .tileProvider(cloudTileProvider)
                    .fadeIn(true)
                    .transparency(OVERLAY_TRANSPARENCY) // *** Đặt độ trong suốt ***
                    .zIndex(3f));
            cloudOverlay.setVisible(false);
            Log.d(TAG, "Cloud overlay added.");
        } else {
            Log.e(TAG, "Failed to create cloud tile provider.");
        }
    }

    // --- Hàm bật/tắt lớp phủ và cập nhật nút ---
    private void toggleOverlay(TileOverlay overlay, Button button) {
        if (overlay != null && button != null) {
            boolean isVisible = !overlay.isVisible(); // Lấy trạng thái mới
            overlay.setVisible(isVisible); // Đặt trạng thái hiển thị mới
            updateButtonState(overlay, button); // Cập nhật giao diện nút
            Log.d(TAG, "Overlay visibility toggled to: " + isVisible);
        } else {
            Log.w(TAG, "Cannot toggle overlay or button is null.");
        }
    }
    // -------------------------------------------

    // --- Hàm cập nhật giao diện nút dựa trên trạng thái lớp phủ ---
    private void updateButtonState(TileOverlay overlay, Button button) {
        if (overlay != null && button != null && getContext() != null) {
            boolean isVisible = overlay.isVisible();
            if (isVisible) {
                // Nếu đang hiển thị: Đặt màu nền khác (ví dụ: màu accent) và chữ trắng
                if (button instanceof MaterialButton) { // Kiểm tra nếu là MaterialButton
                    ((MaterialButton) button).setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.black)); // Ví dụ màu accent
                } else {
                    button.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white)); // Cách cũ hơn
                }
                button.setTextColor(Color.WHITE);
            } else {
                // Nếu đang ẩn: Đặt màu nền mặc định và chữ mặc định
                if (button instanceof MaterialButton) {
                    ((MaterialButton) button).setBackgroundTintList(ContextCompat.getColorStateList(getContext(), com.google.android.material.R.color.material_dynamic_primary95)); // Ví dụ màu nhạt hơn
                } else {
                    button.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white));
                }
                button.setTextColor(Color.BLACK); // Hoặc màu chữ mặc định của theme
            }
        }
    }
    // -------------------------------------------------------------
    private TileProvider createTileProvider(final String layer) {
        try {
            TileProvider tileProvider = new UrlTileProvider(256, 256) {
                @Nullable
                @Override
                public URL getTileUrl(int x, int y, int zoom) {
                    // Kiểm tra API Key trước khi tạo URL
                    if (Constants.API_KEY == null || Constants.API_KEY.isEmpty() || Constants.API_KEY.equals("YOUR_API_KEY")) {
                        Log.e("TileProvider", "OpenWeatherMap API Key is missing or invalid in Constants.java");
                        return null;
                    }
                    String urlStr = String.format(Locale.US, OWM_TILE_URL_FORMAT,
                            layer, zoom, x, y, Constants.API_KEY);
                    try {
                        return new URL(urlStr);
                    } catch (MalformedURLException e) {
                        Log.e("TileProvider", "Malformed URL for layer " + layer + ": " + urlStr, e);
                        return null;
                    }
                }
            };
            return tileProvider;
        } catch (Exception e) {
            Log.e(TAG, "Error creating TileProvider for layer " + layer, e);
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called, removing overlays.");
        if (tempOverlay != null) tempOverlay.remove();
        if (rainOverlay != null) rainOverlay.remove();
        if (cloudOverlay != null) cloudOverlay.remove();
        super.onDestroyView();
    }
}

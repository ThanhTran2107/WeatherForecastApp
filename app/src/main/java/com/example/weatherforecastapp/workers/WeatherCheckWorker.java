package com.example.weatherforecastapp.workers; // Đảm bảo package name chính xác

import android.content.Context;
// import android.location.Location; // Không cần trực tiếp trong Worker này
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.weatherforecastapp.models.CurrentWeather;
import com.example.weatherforecastapp.network.RetrofitClient;
import com.example.weatherforecastapp.network.WeatherApiService;
import com.example.weatherforecastapp.utils.Constants;
import com.example.weatherforecastapp.utils.WeatherNotificationHelper;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class WeatherCheckWorker extends Worker {

    private static final String TAG = "WeatherCheckWorker";
    public static final String KEY_LATITUDE = "latitude"; // Key để truyền vĩ độ
    public static final String KEY_LONGITUDE = "longitude"; // Key để truyền kinh độ

    public WeatherCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Sử dụng ApplicationContext để tránh leak memory
        Context appContext = getApplicationContext();
        Log.i(TAG, "WeatherCheckWorker started execution.");

        // Lấy kinh độ và vĩ độ từ dữ liệu đầu vào của Worker
        Data inputData = getInputData();
        // Cung cấp giá trị mặc định không hợp lệ để dễ kiểm tra
        double latitude = inputData.getDouble(KEY_LATITUDE, -999.0);
        double longitude = inputData.getDouble(KEY_LONGITUDE, -999.0);

        // Kiểm tra xem lat/lon có hợp lệ không
        if (latitude == -999.0 || longitude == -999.0) {
            Log.e(TAG, "Invalid coordinates received from InputData. Lat: " + latitude + ", Lon: " + longitude);
            // Không thể tiếp tục nếu không có tọa độ hợp lệ
            return Result.failure();
        }

        Log.d(TAG, "Fetching weather for coordinates: Lat=" + latitude + ", Lon=" + longitude);

        WeatherApiService apiService = RetrofitClient.getApiService();
        Call<CurrentWeather> call = apiService.getCurrentWeather(
                latitude,
                longitude,
                Constants.API_KEY,
                Constants.UNIT_METRIC // Sử dụng đơn vị metric (Celsius)
        );

        try {
            // Thực hiện cuộc gọi API đồng bộ (Worker chạy trên background thread riêng)
            Response<CurrentWeather> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                CurrentWeather currentWeather = response.body();
                Log.i(TAG, "Successfully fetched weather data.");

                // Đảm bảo kênh thông báo đã được tạo (an toàn khi gọi nhiều lần)
                // Nên gọi createNotificationChannel ở Application class hoặc Activity chính
                // Nhưng gọi ở đây cũng không sao, chỉ là có thể hơi thừa nếu worker chạy thường xuyên.
                WeatherNotificationHelper.createNotificationChannel(appContext);

                // Kiểm tra và gửi thông báo nếu cần
                WeatherNotificationHelper.checkAndNotify(appContext, currentWeather);

                Log.i(TAG, "Weather check completed successfully.");
                return Result.success(); // Báo thành công
            } else {
                // Lỗi từ phía server hoặc API key sai
                String errorBodyStr = "";
                if (response.errorBody() != null) {
                    try {
                        errorBodyStr = response.errorBody().string();
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
                Log.e(TAG, "API call failed: Code=" + response.code() + ", Message=" + response.message() + ", Body=" + errorBodyStr);
                // Nếu lỗi 401 (Unauthorized) hoặc 404 (Not Found) thường là do API key hoặc URL sai -> failure()
                if (response.code() == 401 || response.code() == 404) {
                    return Result.failure();
                }
                // Các lỗi khác có thể thử lại
                return Result.retry();
            }
        } catch (IOException e) {
            // Lỗi mạng hoặc I/O
            Log.e(TAG, "Network or I/O error during API call: " + e.getMessage());
            return Result.retry(); // Thử lại nếu có lỗi mạng
        } catch (Exception e) {
            // Lỗi không xác định khác
            Log.e(TAG, "Unexpected error in WeatherCheckWorker: " + e.getMessage(), e);
            return Result.failure(); // Lỗi không mong muốn, không thử lại
        }
    }
}

package com.example.weatherforecastapp.utils;

import android.Manifest; // Import Manifest
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.weatherforecastapp.MainActivity;
import com.example.weatherforecastapp.R;
import com.example.weatherforecastapp.models.CurrentWeather;

import java.util.List;
import java.util.Locale; // Import Locale
import java.util.Random;

public class WeatherNotificationHelper {

    private static final String CHANNEL_ID = "WEATHER_ALERT_CHANNEL";
    private static final String CHANNEL_NAME = "Cảnh báo thời tiết";
    private static final String CHANNEL_DESC = "Kênh thông báo cho các cảnh báo thời tiết quan trọng";
    private static final String TAG = "WeatherNotification"; // Thêm TAG để ghi log
    private static boolean channelCreated = false; // Cờ để tránh tạo kênh nhiều lần không cần thiết

    /**
     * Tạo kênh thông báo (Cần gọi một lần, ví dụ trong Application class hoặc MainActivity).
     * Chỉ cần thiết cho Android 8.0 (API 26) trở lên.
     * @param context Context ứng dụng.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !channelCreated) {
            try {
                int importance = NotificationManager.IMPORTANCE_HIGH; // Mức độ ưu tiên cao cho cảnh báo
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
                channel.setDescription(CHANNEL_DESC);
                // Đăng ký kênh với hệ thống
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    channelCreated = true; // Đánh dấu đã tạo kênh
                    Log.i(TAG, "Notification channel created successfully.");
                } else {
                    Log.e(TAG, "Failed to get NotificationManager service.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
            }
        }
    }

    /**
     * Kiểm tra dữ liệu thời tiết và gửi thông báo nếu cần.
     * @param context Context ứng dụng (nên dùng ApplicationContext cho Worker).
     * @param weatherData Dữ liệu thời tiết hiện tại.
     */
    public static void checkAndNotify(Context context, CurrentWeather weatherData) {
        if (weatherData == null || weatherData.main == null || weatherData.weather == null || weatherData.weather.isEmpty()) {
            Log.w(TAG, "Invalid weather data received, cannot check for notifications.");
            return;
        }

        double currentTemp = weatherData.main.temp;
        // Lấy điều kiện thời tiết chính (đầu tiên trong danh sách)
        CurrentWeather.Weather mainCondition = weatherData.weather.get(0);
        String description = mainCondition.description != null ? mainCondition.description.toLowerCase(Locale.ROOT) : "";
        // TODO: Bổ sung trường 'id' (int) vào model CurrentWeather.Weather để kiểm tra đáng tin cậy hơn
        // int conditionId = mainCondition.id; // Giả sử bạn đã thêm trường id

        String notificationTitle = null;
        String notificationText = null;
        boolean alertConditionMet = false;

        Log.d(TAG, String.format("Checking weather: Temp=%.1f°C, Desc='%s'", currentTemp, description));

        // 1. Kiểm tra nhiệt độ cực đoan
        if (currentTemp > 35.0) {
            notificationTitle = "Cảnh báo nhiệt độ cao";
            notificationText = String.format(Locale.getDefault(), "Nhiệt độ hiện tại là %.1f°C. Hãy giữ mát!", currentTemp);
            alertConditionMet = true;
            Log.i(TAG, "High temperature alert condition met.");
        } else if (currentTemp < 10.0) {
            notificationTitle = "Cảnh báo nhiệt độ thấp";
            notificationText = String.format(Locale.getDefault(), "Nhiệt độ hiện tại là %.1f°C. Hãy giữ ấm!", currentTemp);
            alertConditionMet = true;
            Log.i(TAG, "Low temperature alert condition met.");
        }

        // 2. Kiểm tra điều kiện thời tiết nguy hiểm (Bão, Mưa lớn) - Ưu tiên kiểm tra này nếu nhiệt độ cũng cực đoan
        // Nên sử dụng ID nếu có thể để chính xác hơn
        // Ví dụ kiểm tra mô tả (ít tin cậy hơn):
        if (description.contains("bão") || description.contains("thunderstorm") || description.contains("lốc xoáy") || description.contains("tornado") || description.contains("hurricane")) {
            notificationTitle = "Cảnh báo thời tiết nguy hiểm";
            notificationText = "Phát hiện thời tiết nguy hiểm (bão/dông) trong khu vực. Hãy cẩn thận!";
            alertConditionMet = true;
            Log.i(TAG, "Severe weather (storm) alert condition met.");
        } else if (description.contains("mưa lớn") || description.contains("heavy rain") || description.contains("mưa rất to") || description.contains("extreme rain") || description.contains("very heavy rain")) {
            // Chỉ ghi đè nếu chưa có cảnh báo nhiệt độ hoặc cảnh báo bão
            if (notificationTitle == null || !notificationTitle.contains("nguy hiểm")) {
                notificationTitle = "Cảnh báo mưa lớn";
                notificationText = "Dự báo có mưa lớn/rất lớn. Hãy chuẩn bị!";
            }
            alertConditionMet = true; // Đánh dấu có điều kiện cảnh báo
            Log.i(TAG, "Heavy rain alert condition met.");
        }


        // Nếu có điều kiện cảnh báo, gửi thông báo
        if (alertConditionMet && notificationTitle != null && notificationText != null) {
            sendNotification(context, notificationTitle, notificationText);
        } else {
            Log.i(TAG, "No alert conditions met for notification.");
        }
    }

    /**
     * Xây dựng và hiển thị thông báo.
     * @param context Context (nên là ApplicationContext).
     * @param title Tiêu đề thông báo.
     * @param text Nội dung thông báo.
     */
    private static void sendNotification(Context context, String title, String text) {
        // Tạo Intent để mở MainActivity khi người dùng nhấn vào thông báo
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Mở activity đã có hoặc tạo mới
        // Tạo PendingIntent - sử dụng FLAG_IMMUTABLE
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0 /* Request code */,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // Quan trọng: Dùng IMMUTABLE
        );

        // Xây dựng thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // TODO: Thay bằng icon thông báo phù hợp (nên là icon trắng trong suốt)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text)) // Cho phép hiển thị nội dung dài hơn
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Ưu tiên cao
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Phân loại là cảnh báo
                .setContentIntent(pendingIntent) // Đặt PendingIntent để mở app
                .setAutoCancel(true) // Tự động hủy thông báo khi nhấn vào
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Hiển thị trên màn hình khóa

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Kiểm tra quyền trước khi gửi (quan trọng cho Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot send notification. POST_NOTIFICATIONS permission not granted.");
                // Trong Worker không thể yêu cầu quyền trực tiếp.
                // MainActivity nên xử lý việc yêu cầu quyền này.
                return; // Không gửi nếu không có quyền
            }
        }

        // notificationId là một số int duy nhất cho mỗi thông báo.
        // Sử dụng một ID cố định nếu bạn muốn thông báo mới ghi đè lên thông báo cũ,
        // hoặc ID ngẫu nhiên nếu muốn hiển thị nhiều thông báo.
        int notificationId = 1; // ID cố định để ghi đè cảnh báo cũ
        // int notificationId = new Random().nextInt(10000); // ID ngẫu nhiên
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.i(TAG, "Notification sent successfully: Title='" + title + "'");
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification", e);
        }
    }
}

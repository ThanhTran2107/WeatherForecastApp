package com.example.weatherforecastapp.models;

import java.util.List;

public class CurrentWeather {
    public Main main;
    public List<Weather> weather;
    public String name; // Tên thành phố (API trả về)
    public Sys sys; // Chứa thông tin quốc gia
    public Coord coord; // Tọa độ trả về từ API

    // Giữ nguyên lớp Main
    public static class Main {
        public double temp;
        public double feels_like; // Nhiệt độ cảm nhận
        public double temp_min;
        public double temp_max;
        public int pressure;
        public int humidity;
    }

    // Cập nhật lớp Weather để có id và icon
    public static class Weather {
        public int id; // ID điều kiện thời tiết (Quan trọng cho việc kiểm tra)
        public String main; // Nhóm điều kiện chính (Rain, Snow, Clouds, etc.)
        public String description; // Mô tả chi tiết
        public String icon; // Mã icon thời tiết
    }

    // Lớp Sys (nếu cần lấy tên quốc gia)
    public static class Sys {
        public String country; // Ví dụ: "VN"
        public long sunrise; // Thời gian mặt trời mọc (Unix timestamp, giây)
        public long sunset;  // Thời gian mặt trời lặn (Unix timestamp, giây)
    }

    // Lớp Coord (tọa độ trả về từ API)
    public static class Coord {
        public double lon;
        public double lat;
    }
}

package com.example.communityalert.data.db;

import java.util.HashMap;
import java.util.Map;

// Lớp POJO (Plain Old Java Object) để đóng gói dữ liệu
public class Alert {
    private String id;  // Firebase document ID
    private String userId;  // User who created this alert
    private String userName;  // Display name of user
    private String description;
    private String type;
    private double latitude;
    private double longitude;
    private String imageUrl;  // Firebase Storage URL
    private long timestamp;  // When alert was created
    private int confirmCount;  // Number of confirmations

    // Constructor rỗng cho Firebase
    public Alert() {
    }

    // Constructor đầy đủ
    public Alert(String id, String userId, String userName, String description, String type,
                 double latitude, double longitude, String imageUrl, long timestamp, int confirmCount) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.description = description;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.confirmCount = confirmCount;
    }

    // Constructor để tạo mới (không có ID)
    public Alert(String userId, String userName, String description, String type,
                 double latitude, double longitude, String imageUrl, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.description = description;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.confirmCount = 0;
    }

    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("description", description);
        map.put("type", type);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("imageUrl", imageUrl);
        map.put("timestamp", timestamp);
        map.put("confirmCount", confirmCount);
        return map;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getConfirmCount() {
        return confirmCount;
    }

    public void setConfirmCount(int confirmCount) {
        this.confirmCount = confirmCount;
    }
}
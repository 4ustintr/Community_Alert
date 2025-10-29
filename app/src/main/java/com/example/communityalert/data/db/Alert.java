package com.example.communityalert.data.db;

import org.osmdroid.util.GeoPoint;

// Lớp POJO (Plain Old Java Object) để đóng gói dữ liệu
public class Alert {
    private long id;
    private String description;
    private String type;
    private GeoPoint location;

    // Constructor để đọc từ DB
    public Alert(long id, String description, String type, GeoPoint location) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.location = location;
    }

    // Constructor để tạo mới
    public Alert(String description, String type, GeoPoint location) {
        this.description = description;
        this.type = type;
        this.location = location;
    }
    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public GeoPoint getLocation() {
        return location;
    }
}
package com.example.communityalert.data.db;

import android.provider.BaseColumns;

// Lớp Contract định nghĩa tên bảng và tên cột
public final class AlertContract {
    private AlertContract() {}

    public static class AlertEntry implements BaseColumns {
        public static final String TABLE_NAME = "alerts";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
    }
}
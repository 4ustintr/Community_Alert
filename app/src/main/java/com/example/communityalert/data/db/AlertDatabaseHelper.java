package com.example.communityalert.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.communityalert.data.db.Alert;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.List;

public class AlertDatabaseHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Alerts.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + AlertContract.AlertEntry.TABLE_NAME + " (" +
                    AlertContract.AlertEntry._ID + " INTEGER PRIMARY KEY," +
                    AlertContract.AlertEntry.COLUMN_NAME_DESCRIPTION + " TEXT," +
                    AlertContract.AlertEntry.COLUMN_NAME_TYPE + " TEXT," +
                    AlertContract.AlertEntry.COLUMN_NAME_LATITUDE + " REAL," +
                    AlertContract.AlertEntry.COLUMN_NAME_LONGITUDE + " REAL)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AlertContract.AlertEntry.TABLE_NAME;

    public AlertDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    // Hàm thêm một cảnh báo mới
    public void addAlert(Alert alert) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AlertContract.AlertEntry.COLUMN_NAME_DESCRIPTION, alert.getDescription());
        values.put(AlertContract.AlertEntry.COLUMN_NAME_TYPE, alert.getType());
        values.put(AlertContract.AlertEntry.COLUMN_NAME_LATITUDE, alert.getLocation().getLatitude());
        values.put(AlertContract.AlertEntry.COLUMN_NAME_LONGITUDE, alert.getLocation().getLongitude());

        db.insert(AlertContract.AlertEntry.TABLE_NAME, null, values);
        db.close();
    }

    // Hàm lấy TẤT CẢ cảnh báo từ DB
    public List<Alert> getAllAlerts() {
        List<Alert> alertList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                AlertContract.AlertEntry.TABLE_NAME, null, null, null, null, null,
                AlertContract.AlertEntry._ID + " DESC" // Sắp xếp theo ID mới nhất
        );

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(AlertContract.AlertEntry._ID));
            String desc = cursor.getString(cursor.getColumnIndexOrThrow(AlertContract.AlertEntry.COLUMN_NAME_DESCRIPTION));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(AlertContract.AlertEntry.COLUMN_NAME_TYPE));
            double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(AlertContract.AlertEntry.COLUMN_NAME_LATITUDE));
            double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(AlertContract.AlertEntry.COLUMN_NAME_LONGITUDE));

            GeoPoint location = new GeoPoint(lat, lon);
            Alert alert = new Alert(id, desc, type, location);
            alertList.add(alert);
        }
        cursor.close();
        db.close();

        return alertList;
    }
}
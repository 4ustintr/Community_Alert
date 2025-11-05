package com.example.communityalert;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

// Thêm "implements OnMapReadyCallback"
public class ConfirmLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng; // Tọa độ nhận được từ MainActivity
    private TextView tvAddress;
    private MaterialButton btnCreateAlert, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_location);

        double latitude = getIntent().getDoubleExtra("latitude", 0);
        double longitude = getIntent().getDoubleExtra("longitude", 0);
        selectedLatLng = new LatLng(latitude, longitude);

        // 2. Ánh xạ View
        tvAddress = findViewById(R.id.tv_address);
        btnCreateAlert = findViewById(R.id.btn_create_alert);
        btnCancel = findViewById(R.id.btn_cancel);

        // 3. Khởi tạo bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 4. Cập nhật địa chỉ (phiên bản đơn giản)
        tvAddress.setText(String.format(Locale.getDefault(),
                "Lat: %.4f, Lng: %.4f", latitude, longitude));

        // 5. Cài đặt nút bấm
        btnCancel.setOnClickListener(v -> {
            finish();
        });

        btnCreateAlert.setOnClickListener(v -> {
            Intent intent = new Intent(ConfirmLocationActivity.this, CreateAlertActivity.class);

            // Gửi tọa độ đã CHỌN qua cho màn hình CreateAlert
            intent.putExtra("latitude", selectedLatLng.latitude);
            intent.putExtra("longitude", selectedLatLng.longitude);

            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 16f));

        // chúng ta cập nhật lại tọa độ và địa chỉ
        mMap.setOnCameraIdleListener(() -> {
            // Lấy tọa độ ở tâm bản đồ (vị trí cái ghim)
            selectedLatLng = mMap.getCameraPosition().target;

            tvAddress.setText(String.format(Locale.getDefault(),
                    "Lat: %.4f, Lng: %.4f",
                    selectedLatLng.latitude, selectedLatLng.longitude));

        });
    }
}
package com.example.communityalert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.communityalert.data.db.Alert;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap mMap;
    private ListenerRegistration alertListener;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private List<Alert> alertList = new ArrayList<>();
    private Map<String, Marker> alertMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup buttons
        setupButtons();
    }

    private void setupButtons() {
        // Create Alert FAB
        FloatingActionButton fabCreateAlert = findViewById(R.id.fab_create_alert);
        fabCreateAlert.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateAlertActivity.class);
            startActivity(intent);
        });

        // My Alerts Button
        findViewById(R.id.btnAlerts).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MyAlertActivity.class);
            startActivity(intent);
        });

        // Profile Button
        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Notification Button
        findViewById(R.id.btn_notify).setOnClickListener(v -> {
            Toast.makeText(this, "Notifications feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map ready");

        // Configure map
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Default location (Hanoi, Vietnam)
        LatLng hanoi = new LatLng(21.0285, 105.8542);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hanoi, 12));

        // Marker click listener
        mMap.setOnMarkerClickListener(marker -> {
            String alertId = (String) marker.getTag();
            if (alertId != null) {
                showAlertDetails(alertId);
                return true;
            }
            return false;
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Intent intent = new Intent(MainActivity.this, ConfirmLocationActivity.class);

                // Gửi tọa độ người dùng vừa nhấn qua Intent
                intent.putExtra("latitude", latLng.latitude);
                intent.putExtra("longitude", latLng.longitude);

                startActivity(intent);
            }
        });

        // Enable My Location
        checkLocationPermission();

        listenForAlerts();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission needed to show your position",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void enableMyLocation() {
        if (mMap == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } catch (SecurityException e) {
            Log.e(TAG, "Error enabling location", e);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && mMap != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(),
                                location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                        Log.d(TAG, "User location updated");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get location", e));
    }

    private void listenForAlerts() {
        if (db == null) return;

        // Bắt đầu lắng nghe collection "alerts"
        alertListener = db.collection("alerts")
                .addSnapshotListener((snapshots, e) -> {

                    // Xử lý lỗi (nếu có)
                    if (e != null) {
                        Log.w(TAG, "Lỗi khi lắng nghe alerts", e);
                        Toast.makeText(this, "Không thể tải cảnh báo (listener failed)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Nếu không có lỗi, bắt đầu xử lý dữ liệu
                    if (snapshots != null) {
                        alertList.clear(); // Xóa danh sách cục bộ

                        // Lặp qua tất cả dữ liệu nhận được
                        for (QueryDocumentSnapshot document : snapshots) {
                            try {
                                Alert alert = document.toObject(Alert.class);
                                alert.setId(document.getId());
                                alertList.add(alert);
                            } catch (Exception parseError) {
                                Log.w(TAG, "Lỗi khi parsing alert: " + document.getId(), parseError);
                            }
                        }

                        // Gọi hàm vẽ lại bản đồ
                        displayAlertsOnMap();
                        Log.d(TAG, "Đã tải (real-time) " + alertList.size() + " alerts");

                    } else {
                        Log.d(TAG, "Không tìm thấy alert nào (snapshot rỗng).");
                    }
                });
    }

    private void displayAlertsOnMap() {
        if (mMap == null) return;

        // Clear existing markers
        for (Marker marker : alertMarkers.values()) {
            marker.remove();
        }
        alertMarkers.clear();

        // Add markers for each alert
        for (Alert alert : alertList) {
            try {
                LatLng position = new LatLng(alert.getLatitude(), alert.getLongitude());

                MarkerOptions options = new MarkerOptions()
                        .position(position)
                        .title(alert.getType())
                        .snippet(alert.getDescription())
                        .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(alert.getType())));

                Marker marker = mMap.addMarker(options);
                if (marker != null) {
                    marker.setTag(alert.getId());
                    alertMarkers.put(alert.getId(), marker);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error adding marker for alert: " + alert.getId(), e);
            }
        }

        Log.d(TAG, "Displayed " + alertMarkers.size() + " markers");
    }

    private float getMarkerColor(String type) {
        if (type == null) return BitmapDescriptorFactory.HUE_VIOLET;

        switch (type) {
            case "Flood":
                return BitmapDescriptorFactory.HUE_BLUE;
            case "Fire":
                return BitmapDescriptorFactory.HUE_ORANGE;
            case "Traffic":
                return BitmapDescriptorFactory.HUE_YELLOW;
            case "Accident":
                return BitmapDescriptorFactory.HUE_RED;
            default:
                return BitmapDescriptorFactory.HUE_VIOLET;
        }
    }

    private void showAlertDetails(String alertId) {
        Intent intent = new Intent(this, AlertDetailActivity.class);
        intent.putExtra("alertId", alertId);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
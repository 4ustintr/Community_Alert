package com.example.communityalert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap mMap;
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

        // Community Button
        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            Toast.makeText(this, "Community feature coming soon", Toast.LENGTH_SHORT).show();
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

        // Map click listener for creating new alerts
        mMap.setOnMapClickListener(latLng -> {
            showCreateAlertDialog(latLng);
        });

        // Enable My Location
        checkLocationPermission();

        // Load alerts
        loadAlerts();
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

    private void loadAlerts() {
        db.collection("alerts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        alertList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Alert alert = document.toObject(Alert.class);
                                alert.setId(document.getId());
                                alertList.add(alert);
                            } catch (Exception e) {
                                Log.w(TAG, "Error parsing alert: " + document.getId(), e);
                            }
                        }
                        displayAlertsOnMap();
                        Log.d(TAG, "Loaded " + alertList.size() + " alerts");
                    } else {
                        Log.w(TAG, "Error loading alerts", task.getException());
                        Toast.makeText(this, "Could not load alerts", Toast.LENGTH_SHORT).show();
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

    private void showCreateAlertDialog(LatLng location) {
        // Inflate custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_confirm_location, null);

        // Find views
        TextView tvAddress = dialogView.findViewById(R.id.tv_address);
        MaterialButton btnCreateAlert = dialogView.findViewById(R.id.btn_create_alert);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Get address from coordinates
        getAddressFromLocation(location, tvAddress);

        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set button click listeners
        btnCreateAlert.setOnClickListener(v -> {
            dialog.dismiss();
            // Navigate to CreateAlertActivity with location
            Intent intent = new Intent(MainActivity.this, CreateAlertActivity.class);
            intent.putExtra("latitude", location.latitude);
            intent.putExtra("longitude", location.longitude);
            intent.putExtra("fromMapClick", true);
            startActivity(intent);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        dialog.show();
    }

    private void getAddressFromLocation(LatLng location, TextView tvAddress) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.latitude, location.longitude, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                if (addressText != null) {
                    tvAddress.setText("Address: " + addressText);
                } else {
                    tvAddress.setText("Address: " + String.format(Locale.getDefault(),
                            "%.6f, %.6f", location.latitude, location.longitude));
                }
            } else {
                tvAddress.setText("Address: " + String.format(Locale.getDefault(),
                        "%.6f, %.6f", location.latitude, location.longitude));
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
            tvAddress.setText("Address: " + String.format(Locale.getDefault(),
                    "%.6f, %.6f", location.latitude, location.longitude));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            loadAlerts();
        }
    }
}

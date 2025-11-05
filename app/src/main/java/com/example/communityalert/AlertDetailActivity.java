package com.example.communityalert;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.communityalert.data.db.Alert;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlertDetailActivity extends AppCompatActivity {

    private static final String TAG = "AlertDetailActivity";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ImageView imgAlert;
    private TextView tvType, tvDescription, tvLocation, tvTime, tvUserName, tvConfirmCount;
    private MaterialButton btnConfirm;

    private String alertId;
    private Alert currentAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_detail);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Chi tiết cảnh báo");
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        imgAlert = findViewById(R.id.img_alert);
        tvType = findViewById(R.id.tv_type);
        tvDescription = findViewById(R.id.tv_description);
        tvLocation = findViewById(R.id.tv_location);
        tvTime = findViewById(R.id.tv_time);
        tvUserName = findViewById(R.id.tv_user_name);
        tvConfirmCount = findViewById(R.id.tv_confirm_count);
        btnConfirm = findViewById(R.id.btn_confirm);

        // Get alert ID from intent
        alertId = getIntent().getStringExtra("alertId");

        if (alertId == null) {
            Toast.makeText(this, "Alert not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load alert details
        loadAlertDetails();

        // Confirm button
        btnConfirm.setOnClickListener(v -> confirmAlert());
    }

    private void loadAlertDetails() {
        db.collection("alerts").document(alertId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentAlert = documentSnapshot.toObject(Alert.class);
                        if (currentAlert != null) {
                            currentAlert.setId(documentSnapshot.getId());
                            displayAlertDetails();
                        }
                    } else {
                        Toast.makeText(this, "Alert not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading alert", e);
                    Toast.makeText(this, "Error loading alert", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayAlertDetails() {
        // Type
        tvType.setText(currentAlert.getType());

        // Description
        tvDescription.setText(currentAlert.getDescription());

        // Location
        String location = String.format(Locale.getDefault(),
                "Lat: %.4f, Lng: %.4f",
                currentAlert.getLatitude(),
                currentAlert.getLongitude());
        tvLocation.setText(location);

        // Time
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String time = sdf.format(new Date(currentAlert.getTimestamp()));
        tvTime.setText(time);

        // User name
        tvUserName.setText("Đăng bởi: " + currentAlert.getUserName());

        // Confirm count
        tvConfirmCount.setText(currentAlert.getConfirmCount() + " lượt xác nhận");

        // Image
        if (currentAlert.getImageUrl() != null && !currentAlert.getImageUrl().isEmpty()) {
            imgAlert.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(currentAlert.getImageUrl())
                    .into(imgAlert);
        } else {
            imgAlert.setVisibility(View.GONE);
        }
    }

    private void confirmAlert() {
        if (currentAlert == null) return;

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Confirming...");

        // Increment confirm count
        int newCount = currentAlert.getConfirmCount() + 1;

        db.collection("alerts").document(alertId)
                .update("confirmCount", newCount)
                .addOnSuccessListener(aVoid -> {
                    currentAlert.setConfirmCount(newCount);
                    tvConfirmCount.setText(newCount + " confirmations");
                    Toast.makeText(this, "Xác nhận cảnh báo!", Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Alert");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error confirming alert", e);
                    Toast.makeText(this, "Failed to confirm alert", Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Alert");
                });
    }
}

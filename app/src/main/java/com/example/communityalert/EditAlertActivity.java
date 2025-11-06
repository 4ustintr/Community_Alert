package com.example.communityalert;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditAlertActivity extends AppCompatActivity {

    private static final String TAG = "EditAlertActivity";

    private ChipGroup chipGroupCategory;
    private TextInputEditText etDetails;
    private MaterialButton btnUpdateAlert;

    private FirebaseFirestore db;
    private String alertId;
    private String currentType;
    private String currentDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_alert);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        alertId = intent.getStringExtra("alertId");
        currentType = intent.getStringExtra("alertType");
        currentDescription = intent.getStringExtra("alertDescription");

        if (alertId == null || currentType == null || currentDescription == null) {
            Toast.makeText(this, "Error: Missing alert data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Alert");
        toolbar.setNavigationOnClickListener(v -> finish());

        chipGroupCategory = findViewById(R.id.chip_group_category);
        etDetails = findViewById(R.id.et_details);
        btnUpdateAlert = findViewById(R.id.btn_post_alert);

        findViewById(R.id.btn_add_photo).setVisibility(android.view.View.GONE);
        findViewById(R.id.img_map_snapshot).setVisibility(android.view.View.GONE);

        etDetails.setText(currentDescription);
        btnUpdateAlert.setText("Update Alert");

        selectCurrentTypeChip();

        btnUpdateAlert.setOnClickListener(v -> updateAlert());
    }

    private void selectCurrentTypeChip() {
        for (int i = 0; i < chipGroupCategory.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupCategory.getChildAt(i);
            if (chip.getText().toString().equals(currentType)) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private void updateAlert() {
        int selectedChipId = chipGroupCategory.getCheckedChipId();
        if (selectedChipId == -1) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip selectedChip = findViewById(selectedChipId);
        String newType = selectedChip.getText().toString();

        String newDescription = etDetails.getText().toString().trim();
        if (TextUtils.isEmpty(newDescription)) {
            etDetails.setError("Description is required");
            return;
        }

        btnUpdateAlert.setEnabled(false);
        btnUpdateAlert.setText("Updating...");

        db.collection("alerts").document(alertId)
                .update("type", newType, "description", newDescription)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Alert updated successfully");
                    Toast.makeText(this, "Alert updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating alert", e);
                    Toast.makeText(this, "Failed to update alert: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    btnUpdateAlert.setEnabled(true);
                    btnUpdateAlert.setText("Update Alert");
                });
    }
}

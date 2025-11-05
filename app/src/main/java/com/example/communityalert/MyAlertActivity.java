package com.example.communityalert;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.text.TextUtils;
import com.bumptech.glide.Glide;
import com.example.communityalert.adapter.AlertHistoryAdapter;
import com.example.communityalert.data.db.Alert;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

public class MyAlertActivity extends AppCompatActivity {

    private static final String TAG = "MyAlertActivity";

    private RecyclerView recyclerView;
    private AlertHistoryAdapter adapter;
    private List<Alert> alertList;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri selectedImageUri;
    private Uri photoUri;
    private AlertDialog currentEditDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_alert);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        recyclerView = findViewById(R.id.recycler_view_alerts);
        Toolbar toolbar = findViewById(R.id.toolbar);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize empty list
        alertList = new ArrayList<>();
        adapter = new AlertHistoryAdapter(this, alertList);
        recyclerView.setAdapter(adapter);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        updateImageInDialog();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        selectedImageUri = photoUri;
                        updateImageInDialog();
                    }
                }
        );

        adapter.setOnItemClickListener(alert -> {
            // Navigate to AlertDetailActivity
            Intent intent = new Intent(MyAlertActivity.this, AlertDetailActivity.class);
            intent.putExtra("alertId", alert.getId());
            startActivity(intent);
        });

        adapter.setOnEditClickListener(alert -> {
            showEditAlertDialog(alert);
        });

        adapter.setOnDeleteClickListener(alert -> {
            showDeleteConfirmDialog(alert);
        });

        // Load alerts from Firestore
        loadMyAlertsFromFirestore();
    }

    private void showDeleteConfirmDialog(Alert alert) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Alert")
                .setMessage("Are you sure you want to delete this alert?\n\n" +
                        "Type: " + alert.getType() + "\n" +
                        "Description: " + alert.getDescription())
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteAlert(alert);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAlert(Alert alert) {
        db.collection("alerts").document(alert.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Alert deleted successfully", Toast.LENGTH_SHORT).show();
                    alertList.remove(alert);
                    adapter.notifyDataSetChanged();
                    
                    if (alertList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("No alerts yet.\nCreate your first alert!");
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting alert", e);
                    Toast.makeText(this, "Failed to delete alert: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMyAlertsFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        // Show empty state initially
        tvEmptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Query Firestore for alerts created by current user
        // Note: Removed orderBy to avoid needing a composite index
        db.collection("alerts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        alertList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Alert alert = document.toObject(Alert.class);
                            alert.setId(document.getId());
                            alertList.add(alert);
                        }

                        // Sort by timestamp in memory (newest first)
                        alertList.sort((a1, a2) -> Long.compare(a2.getTimestamp(), a1.getTimestamp()));

                        if (alertList.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            tvEmptyState.setText("No alerts yet.\nCreate your first alert!");
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }

                        Log.d(TAG, "Loaded " + alertList.size() + " alerts for user");
                    } else {
                        Log.w(TAG, "Error getting alerts", task.getException());
                        Toast.makeText(this, "Failed to load alerts: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        tvEmptyState.setText("Error loading alerts");
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload alerts when returning to this activity
        loadMyAlertsFromFirestore();
    }

    private void showEditAlertDialog(Alert alert) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_edit_alert, null);

        // Find views
        ImageView imgCurrentPhoto = dialogView.findViewById(R.id.img_current_photo);
        ChipGroup chipGroupCategory = dialogView.findViewById(R.id.chip_group_category);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);

        // Set current values
        if (alert.getImageUrl() != null && !alert.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(alert.getImageUrl())
                    .placeholder(R.drawable.ic_baseline_image_24)
                    .into(imgCurrentPhoto);
        }

        // Select current category
        selectCategoryChip(chipGroupCategory, alert.getType());
        etDescription.setText(alert.getDescription());

        // Reset selected image
        selectedImageUri = null;

        // Create dialog
        currentEditDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set button listeners
        dialogView.findViewById(R.id.btn_change_photo).setOnClickListener(v -> {
            showImagePickerDialog();
        });

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            currentEditDialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            saveAlertChanges(alert, chipGroupCategory, etDescription, imgCurrentPhoto);
        });

        currentEditDialog.show();
    }

    private void selectCategoryChip(ChipGroup chipGroup, String category) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.getText().toString().equals(category)) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Change Photo")
                .setItems(new String[]{"Take Photo", "Choose from Gallery", "Remove Photo"}, 
                    (dialog, which) -> {
                        switch (which) {
                            case 0:
                                openCamera();
                                break;
                            case 1:
                                openGallery();
                                break;
                            case 2:
                                selectedImageUri = Uri.parse("remove");
                                updateImageInDialog();
                                break;
                        }
                    })
                .show();
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this,
                    "com.example.communityalert.fileprovider",
                    photoFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir("Pictures");
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void updateImageInDialog() {
        if (currentEditDialog != null && currentEditDialog.isShowing()) {
            ImageView imgCurrentPhoto = currentEditDialog.findViewById(R.id.img_current_photo);
            if (selectedImageUri != null) {
                if (selectedImageUri.toString().equals("remove")) {
                    imgCurrentPhoto.setImageResource(R.drawable.ic_baseline_image_24);
                } else {
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.ic_baseline_image_24)
                            .into(imgCurrentPhoto);
                }
            }
        }
    }

    private void saveAlertChanges(Alert alert, ChipGroup chipGroup, TextInputEditText etDescription, ImageView imgPhoto) {
        // Get selected category
        int selectedChipId = chipGroup.getCheckedChipId();
        if (selectedChipId == -1) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip selectedChip = currentEditDialog.findViewById(selectedChipId);
        String newType = selectedChip.getText().toString();

        // Get description
        String newDescription = etDescription.getText().toString().trim();
        if (TextUtils.isEmpty(newDescription)) {
            etDescription.setError("Description is required");
            return;
        }

        // Disable save button
        currentEditDialog.findViewById(R.id.btn_save).setEnabled(false);

        // Check if image needs to be updated
        if (selectedImageUri != null) {
            if (selectedImageUri.toString().equals("remove")) {
                // Remove image
                updateAlertInFirestore(alert, newType, newDescription, null);
            } else {
                // Upload new image
                uploadImageAndUpdateAlert(alert, newType, newDescription);
            }
        } else {
            // No image change, just update text
            updateAlertInFirestore(alert, newType, newDescription, alert.getImageUrl());
        }
    }

    private void uploadImageAndUpdateAlert(Alert alert, String newType, String newDescription) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String imageName = "alerts/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(imageName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        updateAlertInFirestore(alert, newType, newDescription, imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed", e);
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    currentEditDialog.findViewById(R.id.btn_save).setEnabled(true);
                });
    }

    private void updateAlertInFirestore(Alert alert, String newType, String newDescription, String imageUrl) {
        db.collection("alerts").document(alert.getId())
                .update("type", newType, "description", newDescription, "imageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Alert updated successfully!", Toast.LENGTH_SHORT).show();
                    currentEditDialog.dismiss();
                    
                    // Update local data
                    alert.setType(newType);
                    alert.setDescription(newDescription);
                    alert.setImageUrl(imageUrl);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating alert", e);
                    Toast.makeText(this, "Failed to update alert", Toast.LENGTH_SHORT).show();
                    currentEditDialog.findViewById(R.id.btn_save).setEnabled(true);
                });
    }
}
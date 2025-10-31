package com.example.communityalert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.communityalert.data.db.Alert;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class CreateAlertActivity extends AppCompatActivity {

    private static final String TAG = "CreateAlertActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 3;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 4;

    private ChipGroup chipGroupCategory;
    private TextInputEditText etDetails;
    private MaterialButton btnAddPhoto, btnPostAlert;
    private ImageView imgMapSnapshot;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FusedLocationProviderClient fusedLocationClient;

    private Uri selectedImageUri;
    private double currentLatitude = 0;
    private double currentLongitude = 0;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_alert);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        chipGroupCategory = findViewById(R.id.chip_group_category);
        etDetails = findViewById(R.id.et_details);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        btnPostAlert = findViewById(R.id.btn_post_alert);
        imgMapSnapshot = findViewById(R.id.img_map_snapshot);

        // Setup image picker launchers
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        displaySelectedImage();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        selectedImageUri = photoUri;
                        displaySelectedImage();
                    }
                }
        );

        // Get current location
        checkLocationPermission();

        // Add photo button
        btnAddPhoto.setOnClickListener(v -> showImagePickerDialog());

        // Post alert button
        btnPostAlert.setOnClickListener(v -> postAlert());
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission required to create alert", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            Toast.makeText(this, "Location permission required. Please enable in settings.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Requesting current location for alert creation...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        Log.d(TAG, "Got last location: " + currentLatitude + ", " + currentLongitude);
                        Toast.makeText(this, "Location acquired: " +
                            String.format("%.4f, %.4f", currentLatitude, currentLongitude),
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "Last location is null, requesting fresh location");
                        Toast.makeText(this, "Getting your location, please wait...", Toast.LENGTH_SHORT).show();
                        requestFreshLocationForAlert();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location", e);
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    requestFreshLocationForAlert();
                });
    }

    private void showImagePickerDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Add Photo");
        builder.setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
            if (which == 0) {
                openCamera();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    private void openCamera() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }

        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this,
                    "com.example.communityalert.fileprovider",
                    photoFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error creating image file", ex);
        }
    }

    private void openGallery() {
        // Check storage permission for Android 12 and below
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST_CODE);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir("Pictures");
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .into(imgMapSnapshot);
            btnAddPhoto.setText("Change Photo");
        }
    }

    private void postAlert() {
        // Get selected category
        int selectedChipId = chipGroupCategory.getCheckedChipId();
        if (selectedChipId == -1) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip selectedChip = findViewById(selectedChipId);
        String category = selectedChip.getText().toString();

        // Get description
        String description = etDetails.getText().toString().trim();
        if (TextUtils.isEmpty(description)) {
            etDetails.setError("Description is required");
            return;
        }

        // Check location
        if (currentLatitude == 0 && currentLongitude == 0) {
            Toast.makeText(this, "Getting location, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button
        btnPostAlert.setEnabled(false);
        btnPostAlert.setText("Posting...");

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous";

        // Upload image first if selected
        if (selectedImageUri != null) {
            uploadImageAndCreateAlert(userId, userName, category, description);
        } else {
            createAlert(userId, userName, category, description, null);
        }
    }

    private void uploadImageAndCreateAlert(String userId, String userName, String category, String description) {
        StorageReference storageRef = storage.getReference();
        String imageName = "alerts/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(imageName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        createAlert(userId, userName, category, description, imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed", e);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPostAlert.setEnabled(true);
                    btnPostAlert.setText("Post Alert");
                });
    }

    private void createAlert(String userId, String userName, String category, String description, String imageUrl) {
        Alert alert = new Alert(
                userId,
                userName,
                description,
                category,
                currentLatitude,
                currentLongitude,
                imageUrl,
                System.currentTimeMillis()
        );

        db.collection("alerts")
                .add(alert.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Alert created with ID: " + documentReference.getId());
                    Toast.makeText(this, "Alert posted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating alert", e);
                    Toast.makeText(this, "Failed to post alert: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPostAlert.setEnabled(true);
                    btnPostAlert.setText("Post Alert");
                });
    }

    private void requestFreshLocationForAlert() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Log.d(TAG, "Requesting fresh location update...");

        // Request a single fresh location update with high accuracy
        com.google.android.gms.location.LocationRequest locationRequest =
            com.google.android.gms.location.LocationRequest.create()
                .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setExpirationDuration(15000); // 15 seconds timeout

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                new com.google.android.gms.location.LocationCallback() {
                    @Override
                    public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                        if (locationResult != null && locationResult.getLastLocation() != null) {
                            android.location.Location location = locationResult.getLastLocation();
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();
                            Log.d(TAG, "Got fresh location: " + currentLatitude + ", " + currentLongitude);
                            Toast.makeText(CreateAlertActivity.this,
                                "Location acquired: " + String.format("%.4f, %.4f", currentLatitude, currentLongitude),
                                Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Fresh location is also null");
                            Toast.makeText(CreateAlertActivity.this,
                                "Could not get location. Please:\n" +
                                "1. Make sure GPS is ON\n" +
                                "2. Go near a window or outdoors\n" +
                                "3. Wait a few seconds and try again",
                                Toast.LENGTH_LONG).show();
                        }
                    }
                },
                null
        );
    }
}

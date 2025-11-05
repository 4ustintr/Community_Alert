package com.example.communityalert;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextInputEditText etName, etEmail;
    private MaterialButton btnSaveProfile, btnLogout;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Kiểm tra nếu người dùng chưa đăng nhập (dù hiếm)
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar_profile);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        btnLogout = findViewById(R.id.btn_logout);
        progressBar = findViewById(R.id.progress_bar);

        // Cài đặt Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Tải thông tin người dùng
        loadUserProfile();

        // Cài đặt nút bấm
        btnSaveProfile.setOnClickListener(v -> saveUserProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        setLoading(true);

        etEmail.setText(currentUser.getEmail());
        etEmail.setEnabled(false); // Không cho sửa email

        // Tải tên từ Firestore (đã lưu lúc đăng ký)
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        etName.setText(name);
                    } else {
                        // Nếu không có trong Firestore, thử lấy từ Auth
                        etName.setText(currentUser.getDisplayName());
                    }
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user name", e);
                    etName.setText(currentUser.getDisplayName()); // Dùng tạm tên từ Auth
                    setLoading(false);
                });
    }

    private void saveUserProfile() {
        String newName = etName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            etName.setError("Name is required");
            return;
        }

        setLoading(true);

        // Cập nhật tên trong Firebase Authentication
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
        Task<Void> authTask = currentUser.updateProfile(profileUpdates);

        // Cập nhật tên trong Firestore (collection 'users')
        Task<Void> firestoreTask = db.collection("users").document(currentUser.getUid())
                .update("name", newName);

        // Chờ cả 2 tác vụ hoàn thành
        Tasks.whenAllSuccess(authTask, firestoreTask)
                .addOnSuccessListener(list -> {
                    setLoading(false);
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(ProfileActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSaveProfile.setEnabled(false);
            btnLogout.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSaveProfile.setEnabled(true);
            btnLogout.setEnabled(true);
        }
    }
}
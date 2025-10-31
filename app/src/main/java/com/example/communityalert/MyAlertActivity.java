package com.example.communityalert;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.communityalert.data.db.Alert;
import com.example.communityalert.adapter.AlertHistoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyAlertActivity extends AppCompatActivity {

    private static final String TAG = "MyAlertActivity";

    private RecyclerView recyclerView;
    private AlertHistoryAdapter adapter;
    private List<Alert> alertList;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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

        // Set click listener
        adapter.setOnItemClickListener(alert -> {
            // Navigate to AlertDetailActivity
            Intent intent = new Intent(MyAlertActivity.this, AlertDetailActivity.class);
            intent.putExtra("alertId", alert.getId());
            startActivity(intent);
        });

        // Load alerts from Firestore
        loadMyAlertsFromFirestore();
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
}
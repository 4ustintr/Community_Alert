package com.example.communityalert;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;

import com.example.communityalert.R;
import com.example.communityalert.data.db.AlertDatabaseHelper;
import com.example.communityalert.data.db.Alert;
import com.example.communityalert.adapter.AlertHistoryAdapter;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class MyAlertActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    AlertHistoryAdapter adapter;
    List<Alert> alertList;
    AlertDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_alert);

        recyclerView = findViewById(R.id.recycler_view_alerts);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new AlertDatabaseHelper(this);
        alertList = dbHelper.getAllAlerts();

        // mockup
        if (alertList.isEmpty()) {
            dbHelper.addAlert(new Alert("Ngập 50cm", "Flood", new GeoPoint(21.0, 105.8)));
            dbHelper.addAlert(new Alert("Tắc đường nghiêm trọng", "Traffic", new GeoPoint(21.1, 105.9)));
            alertList = dbHelper.getAllAlerts(); // Tải lại
        }

        adapter = new AlertHistoryAdapter(this, alertList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new AlertHistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Alert alert) {
                showAlertDialog(alert);
            }
        });
    }

    private void showAlertDialog(Alert alert) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(alert.getType()); // Tiêu đề là "Flood", "Traffic"...

        String message = "Mô tả: " + alert.getDescription() + "\n\n"
                + "Vị trí: " + "\n"
                + "   Vĩ độ: " + alert.getLocation().getLatitude() + "\n"
                + "   Kinh độ: " + alert.getLocation().getLongitude();

        builder.setMessage(message);

        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
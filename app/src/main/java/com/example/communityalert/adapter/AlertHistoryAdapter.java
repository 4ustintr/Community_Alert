package com.example.communityalert.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityalert.R;
import com.example.communityalert.data.db.Alert;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertHistoryAdapter extends RecyclerView.Adapter<AlertHistoryAdapter.ViewHolder> {

    private Context context;
    private List<Alert> alertList;
    private OnItemClickListener onItemClickListener;
    private OnEditClickListener onEditClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnItemClickListener {
        void onItemClick(Alert alert);
    }

    public interface OnEditClickListener {
        void onEditClick(Alert alert);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Alert alert);
    }

    public AlertHistoryAdapter(Context context, List<Alert> alertList) {
        this.context = context;
        this.alertList = alertList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.onEditClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alert alert = alertList.get(position);

        holder.tvAlertType.setText(alert.getType());
        holder.tvDescription.setText(alert.getDescription());
        holder.tvConfirmCount.setText(alert.getConfirmCount() + " confirmations");

        // Format location
        String location = String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f",
                alert.getLatitude(), alert.getLongitude());
        holder.tvLocation.setText(location);

        // Format timestamp
        Date date = new Date(alert.getTimestamp());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvTimestamp.setText(sdf.format(date));

        // Item click listener
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(alert);
            }
        });

        // Edit button click listener
        holder.btnEdit.setOnClickListener(v -> {
            if (onEditClickListener != null) {
                onEditClickListener.onEditClick(alert);
            }
        });

        // Delete button click listener
        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(alert);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlertType, tvDescription, tvLocation, tvTimestamp, tvConfirmCount;
        MaterialButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlertType = itemView.findViewById(R.id.tv_alert_type);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvConfirmCount = itemView.findViewById(R.id.tv_confirm_count);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
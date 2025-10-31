package com.example.communityalert.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.communityalert.R;
import com.example.communityalert.data.db.Alert;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertHistoryAdapter extends RecyclerView.Adapter<AlertHistoryAdapter.AlertViewHolder> {

    private Context context;
    private List<Alert> alertList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Alert alert);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public AlertHistoryAdapter(Context context, List<Alert> alertList) {
        this.context = context;
        this.alertList = alertList;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert_history, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alert alert = alertList.get(position);

        // Display type
        holder.tvTitle.setText(alert.getType());

        // Display description
        holder.tvLocation.setText(alert.getDescription());

        // Display formatted time
        if (alert.getTimestamp() > 0) {
            String timeAgo = getTimeAgo(alert.getTimestamp());
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("Just now");
        }
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else if (days < 7) {
            return days + "d ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public class AlertViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon, imgChevron;
        TextView tvTitle, tvLocation, tvTime;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_alert_icon);
            imgChevron = itemView.findViewById(R.id.img_chevron);
            tvTitle = itemView.findViewById(R.id.tv_alert_title);
            tvLocation = itemView.findViewById(R.id.tv_alert_location);
            tvTime = itemView.findViewById(R.id.tv_alert_time);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(alertList.get(position));
                    }
                }
            });
        }
    }
}
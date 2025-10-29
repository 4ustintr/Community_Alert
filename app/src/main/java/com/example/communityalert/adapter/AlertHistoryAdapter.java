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
import java.util.List;

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

        holder.tvTitle.setText(alert.getType()); // Hiển thị Loại
        holder.tvLocation.setText(alert.getDescription()); // Hiển thị Mô tả

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
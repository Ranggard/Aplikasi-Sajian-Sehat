package com.example.sajiansehat.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sajiansehat.R;
import com.example.sajiansehat.models.HistoryItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryItem> historyList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HistoryItem item);
    }

    public HistoryAdapter(List<HistoryItem> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        
        String title = capitalize(item.getTipe()) + " • " + formatDurasi(item.getDurasi());
        holder.tvTipeDurasi.setText(title);

        if (item.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy - HH:mm", new Locale("id", "ID"));
            holder.tvTimestamp.setText(sdf.format(item.getTimestamp().toDate()));
        } else {
            holder.tvTimestamp.setText("-");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void updateData(List<HistoryItem> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String formatDurasi(String durasi) {
        if (durasi == null) return "";
        switch (durasi) {
            case "1_minggu": return "1 Minggu";
            case "1_bulan": return "1 Bulan";
            case "1_hari": return "1 Hari";
            default: return durasi;
        }
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipeDurasi;
        TextView tvTimestamp;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipeDurasi = itemView.findViewById(R.id.tvTipeDurasi);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}

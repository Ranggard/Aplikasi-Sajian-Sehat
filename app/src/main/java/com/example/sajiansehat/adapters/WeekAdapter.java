package com.example.sajiansehat.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sajiansehat.R;

import java.util.List;

public class WeekAdapter extends RecyclerView.Adapter<WeekAdapter.WeekViewHolder> {

    private int totalWeeks;
    private int selectedWeekIndex = 0;
    private OnWeekClickListener listener;

    public interface OnWeekClickListener {
        void onWeekClick(int weekIndex);
    }

    public WeekAdapter(int totalWeeks, OnWeekClickListener listener) {
        this.totalWeeks = totalWeeks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_week_tab, parent, false);
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        holder.tvWeekLabel.setText("MINGGU " + (position + 1));

        if (position == selectedWeekIndex) {
            holder.llWeekContainer.setBackgroundResource(R.drawable.bg_pill_black);
            holder.tvWeekLabel.setTextColor(holder.itemView.getContext().getColor(android.R.color.white));
        } else {
            holder.llWeekContainer.setBackgroundResource(R.drawable.bg_pill_gray);
            holder.tvWeekLabel.setTextColor(holder.itemView.getContext().getColor(R.color.gray_500));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousIndex = selectedWeekIndex;
            selectedWeekIndex = holder.getAdapterPosition();
            notifyItemChanged(previousIndex);
            notifyItemChanged(selectedWeekIndex);
            listener.onWeekClick(selectedWeekIndex);
        });
    }

    @Override
    public int getItemCount() {
        return totalWeeks;
    }

    static class WeekViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llWeekContainer;
        TextView tvWeekLabel;

        public WeekViewHolder(@NonNull View itemView) {
            super(itemView);
            llWeekContainer = itemView.findViewById(R.id.llWeekContainer);
            tvWeekLabel = itemView.findViewById(R.id.tvWeekLabel);
        }
    }
}

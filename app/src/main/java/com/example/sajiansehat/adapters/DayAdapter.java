package com.example.sajiansehat.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sajiansehat.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private int totalDaysInWeek;
    private int startDayOffset; // e.g., if we are in week 2, startDayOffset = 7
    private int selectedDayIndex = 0; // Relative to the current week (0 to 6)
    private OnDayClickListener listener;
    private Date startDate;

    public interface OnDayClickListener {
        void onDayClick(int absoluteDayIndex);
    }

    public DayAdapter(int totalDaysInWeek, int startDayOffset, Date startDate, OnDayClickListener listener) {
        this.totalDaysInWeek = totalDaysInWeek;
        this.startDayOffset = startDayOffset;
        this.startDate = startDate;
        this.listener = listener;
    }

    // Update day list when switching weeks (resets selection to first day)
    public void updateData(int totalDaysInWeek, int startDayOffset) {
        this.totalDaysInWeek = totalDaysInWeek;
        this.startDayOffset = startDayOffset;
        this.selectedDayIndex = 0; // Reset to first day of new week
        notifyDataSetChanged();
    }

    // Get absolute day index of currently selected day
    public int getSelectedAbsoluteDayIndex() {
        return startDayOffset + selectedDayIndex;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_tab, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int absoluteDayIndex = startDayOffset + position;
        int displayHariKe = absoluteDayIndex + 1;

        // Calculate actual calendar date for selected day
        Calendar calendar = Calendar.getInstance();
        if (startDate != null) {
            calendar.setTime(startDate);
        }
        calendar.add(Calendar.DAY_OF_YEAR, absoluteDayIndex);
        Date targetDate = calendar.getTime();

        if (position == selectedDayIndex) {
            // Active state: show green pill with full date information
            holder.llDayContainer.setBackgroundResource(R.drawable.bg_circle_green);
            
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("id", "ID"));
            SimpleDateFormat numFormat = new SimpleDateFormat("dd", new Locale("id", "ID"));
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMM yyyy", new Locale("id", "ID"));
            
            holder.tvDayName.setText(dayFormat.format(targetDate));
            holder.tvDayName.setTextColor(holder.itemView.getContext().getColor(android.R.color.white));
            holder.tvDayName.setTextSize(10f);
            
            holder.tvDateNumber.setText(numFormat.format(targetDate));
            holder.tvDateNumber.setVisibility(View.VISIBLE);
            
            holder.tvMonthYear.setText(monthYearFormat.format(targetDate).toUpperCase());
            holder.tvMonthYear.setVisibility(View.VISIBLE);
        } else {
            // Inactive state: show outlined circle with day number
            holder.llDayContainer.setBackgroundResource(R.drawable.bg_circle_outlined);
            
            holder.tvDayName.setText("Hari " + displayHariKe);
            holder.tvDayName.setTextColor(holder.itemView.getContext().getColor(R.color.gray_500));
            holder.tvDayName.setTextSize(12f);
            
            holder.tvDateNumber.setVisibility(View.GONE);
            holder.tvMonthYear.setVisibility(View.GONE);
        }

        // Handle day selection click
        holder.itemView.setOnClickListener(v -> {
            int previousIndex = selectedDayIndex;
            selectedDayIndex = holder.getAdapterPosition();
            notifyItemChanged(previousIndex);
            notifyItemChanged(selectedDayIndex);
            listener.onDayClick(startDayOffset + selectedDayIndex);
        });
    }

    @Override
    public int getItemCount() {
        return totalDaysInWeek;
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llDayContainer;
        TextView tvDayName, tvDateNumber, tvMonthYear;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            llDayContainer = itemView.findViewById(R.id.llDayContainer);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvDateNumber = itemView.findViewById(R.id.tvDateNumber);
            tvMonthYear = itemView.findViewById(R.id.tvMonthYear);
        }
    }
}

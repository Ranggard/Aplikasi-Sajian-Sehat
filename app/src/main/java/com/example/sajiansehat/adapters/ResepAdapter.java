package com.example.sajiansehat.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sajiansehat.R;
import com.example.sajiansehat.models.ResepItem;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ResepAdapter extends RecyclerView.Adapter<ResepAdapter.ResepViewHolder> {

    private List<ResepItem> listResep;
    private boolean[] expandedStates;

    public ResepAdapter(List<ResepItem> listResep) {
        this.listResep = listResep;
        this.expandedStates = new boolean[listResep != null ? listResep.size() : 0];
    }

    // Method untuk mengupdate daftar resep (karena difilter per hari)
    public void updateData(List<ResepItem> newList) {
        this.listResep = newList;
        this.expandedStates = new boolean[newList != null ? newList.size() : 0];
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_resep, parent, false);
        return new ResepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResepViewHolder holder, int position) {
        ResepItem resep = listResep.get(position);

        holder.tvJudulResep.setText(resep.getTitle());
        holder.tvSumber.setText(resep.getSumber());

        // Pengaturan Waktu Makan (Sarapan, Siang, Malam)
        // Karena yang dipassing kesini selalu 3 menu per hari (atau kurang)
        int mealIndex = position % 3;
        if (mealIndex == 0) {
            holder.tvWaktuMakan.setText("SARAPAN");
            holder.tvWaktuMakan.setTextColor(holder.itemView.getContext().getColor(R.color.orange_600));
        } else if (mealIndex == 1) {
            holder.tvWaktuMakan.setText("MAKAN SIANG");
            holder.tvWaktuMakan.setTextColor(holder.itemView.getContext().getColor(R.color.green_700));
        } else {
            holder.tvWaktuMakan.setText("MAKAN MALAM");
            holder.tvWaktuMakan.setTextColor(holder.itemView.getContext().getColor(R.color.gray_600));
        }

        // Gabungkan nutrisi
        if (resep.getNutrisi() != null && !resep.getNutrisi().isEmpty()) {
            StringBuilder nutrisiStr = new StringBuilder();
            for (int i = 0; i < Math.min(resep.getNutrisi().size(), 3); i++) {
                nutrisiStr.append(resep.getNutrisi().get(i));
                if (i < 2 && i < resep.getNutrisi().size() - 1) {
                    nutrisiStr.append(" • ");
                }
            }
            holder.tvNutrisi.setText(nutrisiStr.toString());
        } else {
            holder.tvNutrisi.setText("Informasi nutrisi tidak tersedia");
        }

        // Format Bahan
        if (resep.getBahan() != null && !resep.getBahan().isEmpty()) {
            StringBuilder bahanStr = new StringBuilder();
            for (String b : resep.getBahan()) {
                bahanStr.append("• ").append(b).append("\n");
            }
            holder.tvBahan.setText(bahanStr.toString().trim());
        } else {
            holder.tvBahan.setText("-");
        }

        // Format Cara Masak
        if (resep.getLangkah() != null && !resep.getLangkah().isEmpty()) {
            StringBuilder langkahStr = new StringBuilder();
            for (int i = 0; i < resep.getLangkah().size(); i++) {
                langkahStr.append(i + 1).append(". ").append(resep.getLangkah().get(i)).append("\n\n");
            }
            holder.tvCaraMasak.setText(langkahStr.toString().trim());
        } else {
            holder.tvCaraMasak.setText("-");
        }

        // Muat gambar
        if (resep.getImage() != null && !resep.getImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(resep.getImage())
                    .centerCrop()
                    .into(holder.imgResep);
        }

        // Logika Dropdown (Expand/Collapse)
        boolean isExpanded = expandedStates[position];
        holder.layoutExpandedContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.tvToggleIcon.setText(isExpanded ? "▲" : "▼");
        holder.tvToggleText.setText(isExpanded ? "Tutup Resep" : "Lihat Resep & Bahan");

        holder.layoutToggleDetail.setOnClickListener(v -> {
            expandedStates[position] = !expandedStates[position];
            notifyItemChanged(position);
        });

        // Tombol YouTube Tutorial
        holder.btnTutorialVideo.setOnClickListener(v -> {
            String query = "Resep " + resep.getTitle();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)));
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listResep != null ? listResep.size() : 0;
    }

    static class ResepViewHolder extends RecyclerView.ViewHolder {
        ImageView imgResep;
        TextView tvJudulResep, tvSumber, tvNutrisi, tvWaktuMakan;
        
        // Expandable Views
        LinearLayout layoutToggleDetail, layoutExpandedContent;
        TextView tvToggleText, tvToggleIcon;
        TextView tvBahan, tvCaraMasak;
        MaterialButton btnTutorialVideo;

        public ResepViewHolder(@NonNull View itemView) {
            super(itemView);
            imgResep = itemView.findViewById(R.id.imgResep);
            tvJudulResep = itemView.findViewById(R.id.tvJudulResep);
            tvSumber = itemView.findViewById(R.id.tvSumber);
            tvNutrisi = itemView.findViewById(R.id.tvNutrisi);
            
            // Waktu Makan kini berada di dalam/overlay gambar
            tvWaktuMakan = itemView.findViewById(R.id.tvWaktuMakan);
            
            layoutToggleDetail = itemView.findViewById(R.id.layoutToggleDetail);
            layoutExpandedContent = itemView.findViewById(R.id.layoutExpandedContent);
            tvToggleText = itemView.findViewById(R.id.tvToggleText);
            tvToggleIcon = itemView.findViewById(R.id.tvToggleIcon);
            
            tvBahan = itemView.findViewById(R.id.tvBahan);
            tvCaraMasak = itemView.findViewById(R.id.tvCaraMasak);
            btnTutorialVideo = itemView.findViewById(R.id.btnTutorialVideo);
        }
    }
}

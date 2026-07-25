package com.example.sajiansehat;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sajiansehat.adapters.DayAdapter;
import com.example.sajiansehat.adapters.ResepAdapter;
import com.example.sajiansehat.adapters.WeekAdapter;
import com.example.sajiansehat.models.RekomendasiResponse;
import com.example.sajiansehat.models.ResepItem;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HasilRekomendasiActivity extends AppCompatActivity {

    private TextView tvDurasiProgram, tvTanggalProgram, tvAnalisisKesehatan, tvSaranNutrisi, tvShowDetail;
    private MaterialButton btnSimpan;
    private boolean isExpanded = false;

    private List<ResepItem> allResep = new ArrayList<>();
    private ResepAdapter resepAdapter;
    private DayAdapter dayAdapter;
    private Date startDate;
    private String currentLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hasil_rekomendasi);

        // Initialize UI views
        tvDurasiProgram = findViewById(R.id.tvDurasiProgram);
        tvTanggalProgram = findViewById(R.id.tvTanggalProgram);
        tvAnalisisKesehatan = findViewById(R.id.tvAnalisisKesehatan);
        tvSaranNutrisi = findViewById(R.id.tvSaranNutrisi);
        tvShowDetail = findViewById(R.id.tvShowDetail);
        RecyclerView rvResep = findViewById(R.id.rvResep);
        RecyclerView rvWeeks = findViewById(R.id.rvWeeks);
        RecyclerView rvDays = findViewById(R.id.rvDays);
        MaterialButton btnKembali = findViewById(R.id.btnKembali);
        btnSimpan = findViewById(R.id.btnSimpan);

        // Hide restaurant section at bottom (now displayed per-recipe)
        View layoutRestaurantSection = findViewById(R.id.layoutRestaurantSection);
        if (layoutRestaurantSection != null) {
            layoutRestaurantSection.setVisibility(View.GONE);
        }

        // Extract intent data
        String jsonResponse = getIntent().getStringExtra("DATA_REKOMENDASI");
        String durasiKey = getIntent().getStringExtra("DURASI");
        String historyId = getIntent().getStringExtra("HISTORY_ID");
        boolean isFromHistory = getIntent().getBooleanExtra("IS_FROM_HISTORY", false);
        
        startDate = Calendar.getInstance().getTime();
        setupHeaderTanggal(durasiKey);
        setupButtonActions(isFromHistory, historyId);

        if (jsonResponse != null) {
            Gson gson = new Gson();
            RekomendasiResponse data = gson.fromJson(jsonResponse, RekomendasiResponse.class);

            if (data.location != null) {
                currentLocation = data.location;
            }

            if (data.getAnalisis() != null) {
                tvAnalisisKesehatan.setText(data.getAnalisis().getAnalisisKesehatan());
                tvSaranNutrisi.setText(data.getAnalisis().getSaranNutrisi());
            }

            if (data.getResep() != null && !data.getResep().isEmpty()) {
                allResep = data.getResep();
                int totalHari = Math.max(1, (int) Math.ceil(allResep.size() / 3.0));

                resepAdapter = new ResepAdapter(getResepForDay(0), currentLocation, null);
                rvResep.setLayoutManager(new LinearLayoutManager(this));
                rvResep.setAdapter(resepAdapter);

                setupDayAndWeekTabs(totalHari, rvWeeks, rvDays);
            } else {
                Toast.makeText(this, getString(R.string.msg_tidak_ada_resep), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.msg_data_kosong), Toast.LENGTH_SHORT).show();
            finish();
        }

        // Toggle expand/collapse details
        tvShowDetail.setOnClickListener(v -> {
            int maxLines = isExpanded ? 3 : Integer.MAX_VALUE;
            int textRes = isExpanded ? R.string.btn_show_detail : R.string.btn_hide_detail;
            tvAnalisisKesehatan.setMaxLines(maxLines);
            tvSaranNutrisi.setMaxLines(maxLines);
            tvShowDetail.setText(textRes);
            isExpanded = !isExpanded;
        });

        btnKembali.setOnClickListener(v -> finish());
    }

    private void setupButtonActions(boolean isFromHistory, String historyId) {
        if (isFromHistory) {
            btnSimpan.setText(R.string.btn_hapus_rekomendasi);
            btnSimpan.setBackgroundColor(getResources().getColor(R.color.red_500, null));
            btnSimpan.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_hapus_title)
                        .setMessage(R.string.dialog_hapus_message)
                        .setPositiveButton(R.string.dialog_ya_hapus, (dialog, which) -> deleteRecommendation(historyId))
                        .setNegativeButton(R.string.dialog_tidak, (dialog, which) -> dialog.dismiss())
                        .show()
            );
        } else {
            btnSimpan.setText(R.string.btn_simpan_menu);
            btnSimpan.setBackgroundColor(getResources().getColor(R.color.green_600, null));
            btnSimpan.setOnClickListener(v -> saveRecommendation());
        }
    }

    private String determineTipeMasakan() {
        if (allResep == null || allResep.isEmpty()) return "Mix";
        boolean hasIndo = false;
        boolean hasInter = false;
        for (ResepItem r : allResep) {
            if ("Indonesia".equalsIgnoreCase(r.getSumber())) hasIndo = true;
            if ("Internasional".equalsIgnoreCase(r.getSumber())) hasInter = true;
        }
        if (hasIndo && hasInter) return "Mix";
        if (hasIndo) return "Indonesia";
        if (hasInter) return "Internasional";
        return "Mix";
    }

    private void saveRecommendation() {
        String jsonResponse = getIntent().getStringExtra("DATA_REKOMENDASI");
        String durasiKey = getIntent().getStringExtra("DURASI");
        
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.msg_login_required_simpan), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSimpan.setEnabled(false);
        btnSimpan.setText(R.string.btn_menyimpan);

        String tipeMasakan = determineTipeMasakan();

        java.util.Map<String, Object> historyData = new java.util.HashMap<>();
        historyData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        historyData.put("durasi", durasiKey != null ? durasiKey : "1_hari");
        historyData.put("tipe", tipeMasakan);
        historyData.put("json_data", jsonResponse);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("history")
                .add(historyData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, getString(R.string.msg_berhasil_disimpan), Toast.LENGTH_SHORT).show();
                    btnSimpan.setText(R.string.btn_saved);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.msg_gagal_simpan) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText(R.string.btn_simpan_menu);
                });
    }

    private void deleteRecommendation(String historyId) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.msg_login_required_hapus), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSimpan.setEnabled(false);
        btnSimpan.setText(R.string.btn_menghapus);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("history")
                .document(historyId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.msg_berhasil_dihapus), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.msg_gagal_hapus) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText(R.string.btn_hapus_rekomendasi);
                });
    }

    private List<ResepItem> getResepForDay(int absoluteDayIndex) {
        List<ResepItem> dayResep = new ArrayList<>();
        int startIndex = absoluteDayIndex * 3;
        for (int i = startIndex; i < startIndex + 3 && i < allResep.size(); i++) {
            dayResep.add(allResep.get(i));
        }
        return dayResep;
    }

    private void setupDayAndWeekTabs(int totalHari, RecyclerView rvWeeks, RecyclerView rvDays) {
        if (totalHari > 7) {
            int totalWeeks = (int) Math.ceil(totalHari / 7.0);

            WeekAdapter weekAdapter = new WeekAdapter(totalWeeks, weekIndex -> {
                int remainingDays = totalHari - (weekIndex * 7);
                int daysInThisWeek = Math.min(remainingDays, 7);
                int dayOffset = weekIndex * 7;
                dayAdapter.updateData(daysInThisWeek, dayOffset);
                filterResepByDay(dayOffset);
            });
            rvWeeks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvWeeks.setAdapter(weekAdapter);
            rvWeeks.setVisibility(RecyclerView.VISIBLE);

            dayAdapter = new DayAdapter(7, 0, startDate, this::filterResepByDay);
        } else {
            rvWeeks.setVisibility(RecyclerView.GONE);
            dayAdapter = new DayAdapter(totalHari, 0, startDate, this::filterResepByDay);
        }

        rvDays.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDays.setAdapter(dayAdapter);
    }

    private void filterResepByDay(int absoluteDayIndex) {
        List<ResepItem> filtered = getResepForDay(absoluteDayIndex);
        resepAdapter.updateData(filtered);
    }

    private void setupHeaderTanggal(String durasiKey) {
        String durasiText = "1 HARI";
        int daysToAdd = 1;

        if (durasiKey != null) {
            switch (durasiKey) {
                case "1_minggu":
                    durasiText = "1 MINGGU";
                    daysToAdd = 7;
                    break;
                case "1_bulan":
                    durasiText = "1 BULAN";
                    daysToAdd = 30;
                    break;
                case "1_hari":
                default:
                    durasiText = "1 HARI";
                    break;
            }
        }

        tvDurasiProgram.setText(durasiText);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
        
        String startDateStr = sdf.format(calendar.getTime()).toUpperCase();
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd - 1);
        String endDate = sdf.format(calendar.getTime()).toUpperCase();

        String tanggalText = (daysToAdd == 1) ? startDateStr : getString(R.string.format_tanggal_range, startDateStr, endDate);
        tvTanggalProgram.setText(tanggalText);
    }
}

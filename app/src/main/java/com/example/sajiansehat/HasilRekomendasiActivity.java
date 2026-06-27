package com.example.sajiansehat;

import android.os.Bundle;
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
    private RecyclerView rvResep, rvWeeks, rvDays;
    private MaterialButton btnKembali, btnSimpan;
    private boolean isExpanded = false;

    private List<ResepItem> allResep = new ArrayList<>();
    private ResepAdapter resepAdapter;
    private DayAdapter dayAdapter;
    private Date startDate;
    private int totalHari;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hasil_rekomendasi);

        // Bind views
        tvDurasiProgram = findViewById(R.id.tvDurasiProgram);
        tvTanggalProgram = findViewById(R.id.tvTanggalProgram);
        tvAnalisisKesehatan = findViewById(R.id.tvAnalisisKesehatan);
        tvSaranNutrisi = findViewById(R.id.tvSaranNutrisi);
        tvShowDetail = findViewById(R.id.tvShowDetail);
        rvResep = findViewById(R.id.rvResep);
        rvWeeks = findViewById(R.id.rvWeeks);
        rvDays = findViewById(R.id.rvDays);
        btnKembali = findViewById(R.id.btnKembali);
        btnSimpan = findViewById(R.id.btnSimpan);

        // Ambil data JSON dan DURASI dari Intent
        String jsonResponse = getIntent().getStringExtra("DATA_REKOMENDASI");
        String durasiKey = getIntent().getStringExtra("DURASI");
        
        startDate = Calendar.getInstance().getTime();
        setupHeaderTanggal(durasiKey);

        if (jsonResponse != null) {
            Gson gson = new Gson();
            RekomendasiResponse data = gson.fromJson(jsonResponse, RekomendasiResponse.class);

            // Set Data Analisis
            if (data.getAnalisis() != null) {
                tvAnalisisKesehatan.setText(data.getAnalisis().getAnalisisKesehatan());
                tvSaranNutrisi.setText(data.getAnalisis().getSaranNutrisi());
            }

            // Simpan semua resep
            if (data.getResep() != null && !data.getResep().isEmpty()) {
                allResep = data.getResep();
                totalHari = Math.max(1, (int) Math.ceil(allResep.size() / 3.0));

                // Setup ResepAdapter (awalnya tampilkan hari 1)
                resepAdapter = new ResepAdapter(getResepForDay(0));
                rvResep.setLayoutManager(new LinearLayoutManager(this));
                rvResep.setAdapter(resepAdapter);

                // Setup Tab Navigasi
                setupDayAndWeekTabs(totalHari);
            } else {
                Toast.makeText(this, "Tidak ada resep ditemukan.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Data rekomendasi kosong!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Logika Show Detail
        tvShowDetail.setOnClickListener(v -> {
            if (isExpanded) {
                tvAnalisisKesehatan.setMaxLines(3);
                tvSaranNutrisi.setMaxLines(3);
                tvShowDetail.setText("Show Detail");
            } else {
                tvAnalisisKesehatan.setMaxLines(Integer.MAX_VALUE);
                tvSaranNutrisi.setMaxLines(Integer.MAX_VALUE);
                tvShowDetail.setText("Hide Detail");
            }
            isExpanded = !isExpanded;
        });

        // Tombol Kembali
        btnKembali.setOnClickListener(v -> finish());

        // Tombol Simpan Rekomendasi
        btnSimpan.setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Anda harus login untuk menyimpan rekomendasi!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable button and show progress text
            btnSimpan.setEnabled(false);
            btnSimpan.setText("Menyimpan...");

            // Get required data
            String tipeMasakan = "Mix"; // default fallback
            if (allResep != null && !allResep.isEmpty()) {
                boolean hasIndo = false;
                boolean hasInter = false;
                for (ResepItem r : allResep) {
                    if ("Indonesia".equalsIgnoreCase(r.getSumber())) hasIndo = true;
                    if ("Internasional".equalsIgnoreCase(r.getSumber())) hasInter = true;
                }
                if (hasIndo && hasInter) tipeMasakan = "Mix";
                else if (hasIndo) tipeMasakan = "Indonesia";
                else if (hasInter) tipeMasakan = "Internasional";
            }

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
                        Toast.makeText(this, "Rekomendasi berhasil disimpan! ✅", Toast.LENGTH_SHORT).show();
                        btnSimpan.setText("Tersimpan");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSimpan.setEnabled(true);
                        btnSimpan.setText("Simpan Menu");
                    });
        });
    }

    /**
     * Mengambil 3 resep untuk hari tertentu (berdasarkan absoluteDayIndex)
     */
    private List<ResepItem> getResepForDay(int absoluteDayIndex) {
        List<ResepItem> dayResep = new ArrayList<>();
        int startIndex = absoluteDayIndex * 3;
        for (int i = startIndex; i < startIndex + 3 && i < allResep.size(); i++) {
            dayResep.add(allResep.get(i));
        }
        return dayResep;
    }

    /**
     * Setup tab Minggu dan Hari
     */
    private void setupDayAndWeekTabs(int totalHari) {
        if (totalHari > 7) {
            // Durasi 1 bulan: bagi menjadi minggu-minggu
            int totalWeeks = (int) Math.ceil(totalHari / 7.0);

            rvWeeks.setVisibility(RecyclerView.VISIBLE);
            WeekAdapter weekAdapter = new WeekAdapter(totalWeeks, weekIndex -> {
                // Saat minggu dipilih, update DayAdapter
                int daysInThisWeek = Math.min(7, totalHari - (weekIndex * 7));
                int dayOffset = weekIndex * 7;
                dayAdapter.updateData(daysInThisWeek, dayOffset);
                // Otomatis pilih hari 1 dari minggu tersebut
                filterResepByDay(dayOffset);
            });
            rvWeeks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvWeeks.setAdapter(weekAdapter);

            // Awalnya tampilkan hari-hari minggu 1
            int daysInFirstWeek = Math.min(7, totalHari);
            dayAdapter = new DayAdapter(daysInFirstWeek, 0, startDate, this::filterResepByDay);
            rvDays.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvDays.setAdapter(dayAdapter);

        } else {
            // Durasi 1 hari atau 1 minggu: hanya tampilkan tab hari
            rvWeeks.setVisibility(RecyclerView.GONE);

            dayAdapter = new DayAdapter(totalHari, 0, startDate, this::filterResepByDay);
            rvDays.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvDays.setAdapter(dayAdapter);
        }
    }

    /**
     * Filter daftar resep untuk hari tertentu
     */
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
                    daysToAdd = 1;
                    break;
            }
        }

        tvDurasiProgram.setText(durasiText);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
        
        String startDateStr = sdf.format(calendar.getTime()).toUpperCase();
        
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd - 1);
        String endDate = sdf.format(calendar.getTime()).toUpperCase();

        if (daysToAdd == 1) {
            tvTanggalProgram.setText(startDateStr);
        } else {
            tvTanggalProgram.setText(startDateStr + " - " + endDate);
        }
    }
}

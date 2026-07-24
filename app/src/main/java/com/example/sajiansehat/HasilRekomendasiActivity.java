package com.example.sajiansehat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sajiansehat.adapters.DayAdapter;
import com.example.sajiansehat.adapters.ResepAdapter;
import com.example.sajiansehat.adapters.RestaurantAdapter;
import com.example.sajiansehat.adapters.WeekAdapter;
import com.example.sajiansehat.models.RekomendasiResponse;
import com.example.sajiansehat.models.ResepItem;
import com.example.sajiansehat.models.RestaurantRecommendation;
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
    private TextView tvRestaurantHeader, tvNoRestaurantResult;
    private RecyclerView rvResep, rvWeeks, rvDays, rvRestaurants;
    private MaterialButton btnKembali, btnSimpan;
    private LinearLayout layoutRestaurantSection;
    private boolean isExpanded = false;

    private List<ResepItem> allResep = new ArrayList<>();
    private List<RestaurantRecommendation> allRestaurants = new ArrayList<>();
    private ResepAdapter resepAdapter;
    private RestaurantAdapter restaurantAdapter;
    private DayAdapter dayAdapter;
    private Date startDate;
    private int totalHari;
    private String currentLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hasil_rekomendasi);

        // Initialize all UI views from layout
        tvDurasiProgram = findViewById(R.id.tvDurasiProgram);
        tvTanggalProgram = findViewById(R.id.tvTanggalProgram);
        tvAnalisisKesehatan = findViewById(R.id.tvAnalisisKesehatan);
        tvSaranNutrisi = findViewById(R.id.tvSaranNutrisi);
        tvShowDetail = findViewById(R.id.tvShowDetail);
        tvRestaurantHeader = findViewById(R.id.tvRestaurantHeader);
        tvNoRestaurantResult = findViewById(R.id.tvNoRestaurantResult);
        rvResep = findViewById(R.id.rvResep);
        rvWeeks = findViewById(R.id.rvWeeks);
        rvDays = findViewById(R.id.rvDays);
        rvRestaurants = findViewById(R.id.rvRestaurants);
        layoutRestaurantSection = findViewById(R.id.layoutRestaurantSection);
        btnKembali = findViewById(R.id.btnKembali);
        btnSimpan = findViewById(R.id.btnSimpan);

        // Extract JSON recommendation data and duration from intent
        String jsonResponse = getIntent().getStringExtra("DATA_REKOMENDASI");
        String durasiKey = getIntent().getStringExtra("DURASI");
        String historyId = getIntent().getStringExtra("HISTORY_ID");
        boolean isFromHistory = getIntent().getBooleanExtra("IS_FROM_HISTORY", false);
        
        startDate = Calendar.getInstance().getTime();
        setupHeaderTanggal(durasiKey);

        // Setup button based on origin (from history atau recommendation baru)
        setupButtonActions(isFromHistory, historyId);

        if (jsonResponse != null) {
            Gson gson = new Gson();
            // Parse JSON response into model object
            RekomendasiResponse data = gson.fromJson(jsonResponse, RekomendasiResponse.class);

            // Store location for restaurant section header
            if (data.location != null) {
                currentLocation = data.location;
            }

            // Display health analysis and nutrition recommendations
            if (data.getAnalisis() != null) {
                tvAnalisisKesehatan.setText(data.getAnalisis().getAnalisisKesehatan());
                tvSaranNutrisi.setText(data.getAnalisis().getSaranNutrisi());
            }

            // Store all recipes and calculate number of days (3 recipes per day)
            if (data.getResep() != null && !data.getResep().isEmpty()) {
                allResep = data.getResep();
                // Calculate total days based on 3 recipes per day
                totalHari = Math.max(1, (int) Math.ceil(allResep.size() / 3.0));

                // Initialize recipe adapter with first day's recipes (tanpa restaurants global)
                resepAdapter = new ResepAdapter(getResepForDay(0), currentLocation, null);
                rvResep.setLayoutManager(new LinearLayoutManager(this));
                rvResep.setAdapter(resepAdapter);

                // Initialize day/week navigation tabs
                setupDayAndWeekTabs(totalHari);
            } else {
                Toast.makeText(this, "Tidak ada resep ditemukan.", Toast.LENGTH_SHORT).show();
            }

            // Hide restaurant section at bottom (now showing per-recipe)
            layoutRestaurantSection.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "Data rekomendasi kosong!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Toggle expand/collapse for analysis and nutrition details
        // Logika Show Detail
        tvShowDetail.setOnClickListener(v -> {
            if (isExpanded) {
                // Collapse details to 3 lines
                tvAnalisisKesehatan.setMaxLines(3);
                tvSaranNutrisi.setMaxLines(3);
                tvShowDetail.setText("Show Detail");
            } else {
                // Expand details to show all lines
                tvAnalisisKesehatan.setMaxLines(Integer.MAX_VALUE);
                tvSaranNutrisi.setMaxLines(Integer.MAX_VALUE);
                tvShowDetail.setText("Hide Detail");
            }
            isExpanded = !isExpanded;
        });

        // Back button to return to previous screen
        btnKembali.setOnClickListener(v -> finish());
    }

    private void setupButtonActions(boolean isFromHistory, String historyId) {
        if (isFromHistory) {
            // From history: change button to DELETE with red color
            btnSimpan.setText("Hapus Rekomendasi");
            btnSimpan.setBackgroundColor(getResources().getColor(R.color.red_500, null));
            
            btnSimpan.setOnClickListener(v -> {
                // Show confirmation dialog before deleting
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Hapus Rekomendasi?")
                        .setMessage("Apakah Anda yakin ingin menghapus rekomendasi ini dari riwayat?")
                        .setPositiveButton("Ya, Hapus", (dialog, which) -> {
                            deleteRecommendation(historyId);
                        })
                        .setNegativeButton("Tidak", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            });
        } else {
            // From new recommendation: keep SAVE button with green color
            btnSimpan.setText("Simpan Menu");
            btnSimpan.setBackgroundColor(getResources().getColor(R.color.green_600, null));
            
            btnSimpan.setOnClickListener(v -> {
                saveRecommendation();
            });
        }
    }

    private void saveRecommendation() {
        String jsonResponse = getIntent().getStringExtra("DATA_REKOMENDASI");
        String durasiKey = getIntent().getStringExtra("DURASI");
        
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Anda harus login untuk menyimpan rekomendasi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state on save button
        btnSimpan.setEnabled(false);
        btnSimpan.setText("Menyimpan...");

        // Determine meal type from recipe sources to classify recommendation
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
                    Toast.makeText(this, "Rekomendasi berhasil disimpan", Toast.LENGTH_SHORT).show();
                    btnSimpan.setText("Saved");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText("Simpan Menu");
                });
    }

    private void deleteRecommendation(String historyId) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Anda harus login untuk menghapus rekomendasi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state on delete button
        btnSimpan.setEnabled(false);
        btnSimpan.setText("Menghapus...");

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("history")
                .document(historyId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Rekomendasi berhasil dihapus", Toast.LENGTH_SHORT).show();
                    // Go back to history after deletion
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText("Hapus Rekomendasi");
                });
    }

    /**
     * Get 3 recipes for a specific day (based on absoluteDayIndex)
     */
    private List<ResepItem> getResepForDay(int absoluteDayIndex) {
        List<ResepItem> dayResep = new ArrayList<>();
        int startIndex = absoluteDayIndex * 3;
        for (int i = startIndex; i < startIndex + 3 && i < allResep.size(); i++) {
            dayResep.add(allResep.get(i));
        }
        return dayResep;
    }

    private void setupDayAndWeekTabs(int totalHari) {
        if (totalHari > 7) {
            // For 1-month duration: divide into weeks (max 7 days per week)
            int totalWeeks = (int) Math.ceil(totalHari / 7.0);

            // Handle week selection and update day tabs
            WeekAdapter weekAdapter = new WeekAdapter(totalWeeks, weekIndex -> {
                int daysInThisWeek = Math.min(7, totalHari - (weekIndex * 7));
                int dayOffset = weekIndex * 7;
                dayAdapter.updateData(daysInThisWeek, dayOffset);
                // Auto-select first day of selected week
                filterResepByDay(dayOffset);
            });
            rvWeeks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvWeeks.setAdapter(weekAdapter);
            rvWeeks.setVisibility(RecyclerView.VISIBLE);

            // Initialize day tabs for first week
            int daysInFirstWeek = Math.min(7, totalHari);
            dayAdapter = new DayAdapter(daysInFirstWeek, 0, startDate, this::filterResepByDay);
            rvDays.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvDays.setAdapter(dayAdapter);

        } else {
            // For 1-day or 1-week duration: show only day tabs (no weeks)
            rvWeeks.setVisibility(RecyclerView.GONE);

            dayAdapter = new DayAdapter(totalHari, 0, startDate, this::filterResepByDay);
            rvDays.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvDays.setAdapter(dayAdapter);
        }
    }

    private void filterResepByDay(int absoluteDayIndex) {
        // Load and display recipes for selected day
        List<ResepItem> filtered = getResepForDay(absoluteDayIndex);
        resepAdapter.updateData(filtered);
    }

    private void setupHeaderTanggal(String durasiKey) {
        // Set duration text and calculate date range
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
        
        String startDateStr = sdf.format(calendar.getTime()).toUpperCase();
        
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd - 1);
        String endDate = sdf.format(calendar.getTime()).toUpperCase();

        // Display single date for 1-day, or date range for longer durations
        if (daysToAdd == 1) {
            tvTanggalProgram.setText(startDateStr);
        } else {
            tvTanggalProgram.setText(startDateStr + " - " + endDate);
        }
    }

    /**
     * Display restaurant recommendations or show error message if none found
     */
    private void displayRestaurantRecommendations(List<RestaurantRecommendation> restaurants, 
            String location, boolean hasError) {
        if (layoutRestaurantSection == null) {
            return;
        }

        // Set header text
        if (tvRestaurantHeader != null && location != null && !location.isEmpty()) {
            tvRestaurantHeader.setText("di " + location);
        }

        if (restaurants == null || restaurants.isEmpty() || hasError) {
            // Show error message
            if (tvNoRestaurantResult != null) {
                tvNoRestaurantResult.setVisibility(View.VISIBLE);
                if (location != null && !location.isEmpty()) {
                    tvNoRestaurantResult.setText("⚠️ Rekomendasi Makan Lokasi " + location + " tersebut tidak ada");
                } else {
                    tvNoRestaurantResult.setText("⚠️ Rekomendasi Makan tersebut tidak ada");
                }
            }
            if (rvRestaurants != null) {
                rvRestaurants.setVisibility(View.GONE);
            }
        } else {
            // Show restaurants list
            if (tvNoRestaurantResult != null) {
                tvNoRestaurantResult.setVisibility(View.GONE);
            }
            if (rvRestaurants != null) {
                rvRestaurants.setVisibility(View.VISIBLE);
                restaurantAdapter = new RestaurantAdapter(restaurants, restaurant -> {
                    openRestaurantInMaps(restaurant);
                });
                rvRestaurants.setLayoutManager(new LinearLayoutManager(this));
                rvRestaurants.setAdapter(restaurantAdapter);
            }
        }

        layoutRestaurantSection.setVisibility(View.VISIBLE);
    }

    /**
     * Open restaurant in Google Maps
     */
    private void openRestaurantInMaps(RestaurantRecommendation restaurant) {
        if (restaurant == null) {
            return;
        }
        
        try {
            // Build search query dengan prioritas: nama + alamat lengkap
            String query = "";
            
            // Strategy 1: Gunakan nama + alamat lengkap (paling akurat)
            if (restaurant.getName() != null && !restaurant.getName().isEmpty() &&
                restaurant.getAddress() != null && !restaurant.getAddress().isEmpty()) {
                query = restaurant.getName() + ", " + restaurant.getAddress();
            }
            // Strategy 2: Jika alamat tidak lengkap, gunakan nama + lokasi + kota
            else if (restaurant.getName() != null && !restaurant.getName().isEmpty() &&
                     restaurant.getLocation() != null && !restaurant.getLocation().isEmpty()) {
                query = restaurant.getName() + ", " + restaurant.getLocation();
            }
            // Strategy 3: Fallback ke nama saja
            else if (restaurant.getName() != null && !restaurant.getName().isEmpty()) {
                query = restaurant.getName();
            }
            else {
                query = "Restoran";
            }
            
            android.util.Log.d("RestaurantMap", "Search query: " + query);
            
            // Build Google Maps search URL dengan query yang sudah di-encode
            // IMPORTANT: Encode properly untuk handle special characters (comma, space, dll)
            String encodedQuery = Uri.encode(query);
            String mapsUrl = "https://www.google.com/maps/search/" + encodedQuery;
            
            android.util.Log.d("RestaurantMap", "Maps URL: " + mapsUrl);
            
            // Try 1: Try opening with geo:0,0 intent (more reliable for Maps)
            try {
                // Build geo intent: geo:0,0?q=search_query
                String geoUrl = "geo:0,0?q=" + encodedQuery;
                Intent geoIntent = new Intent(Intent.ACTION_VIEW);
                geoIntent.setData(Uri.parse(geoUrl));
                geoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                android.util.Log.d("RestaurantMap", "Trying geo intent: " + geoUrl);
                
                if (geoIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(geoIntent);
                    android.util.Log.d("RestaurantMap", "Opened with geo intent");
                    return;
                }
            } catch (Exception e) {
                android.util.Log.d("RestaurantMap", "Geo intent failed: " + e.getMessage());
            }
            
            // Try 2: Direct HTTPS Maps URL (works in browser and Maps)
            try {
                Intent mapsIntent = new Intent(Intent.ACTION_VIEW);
                mapsIntent.setData(Uri.parse(mapsUrl));
                mapsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                android.util.Log.d("RestaurantMap", "Trying direct HTTPS intent");
                
                if (mapsIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapsIntent);
                    android.util.Log.d("RestaurantMap", "Opened with HTTPS intent");
                    return;
                } else {
                    android.util.Log.d("RestaurantMap", "HTTPS intent cannot resolve, trying anyway");
                    // Try anyway - sometimes resolveActivity returns false but intent still works
                    startActivity(mapsIntent);
                    android.util.Log.d("RestaurantMap", "Opened anyway (force start)");
                    return;
                }
            } catch (Exception e) {
                android.util.Log.e("RestaurantMap", "HTTPS intent failed: " + e.getMessage());
            }
            
            // Try 3: If all else fails, show Google Maps homepage
            try {
                Intent fallbackIntent = new Intent(Intent.ACTION_VIEW);
                fallbackIntent.setData(Uri.parse("https://maps.google.com"));
                fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                startActivity(fallbackIntent);
                android.util.Log.d("RestaurantMap", "Opened Google Maps homepage");
            } catch (Exception e) {
                android.util.Log.e("RestaurantMap", "All methods failed: " + e.getMessage());
                Toast.makeText(this, "Tidak bisa membuka Maps", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            android.util.Log.e("RestaurantMap", "Unexpected error: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

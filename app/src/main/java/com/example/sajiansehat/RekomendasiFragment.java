package com.example.sajiansehat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sajiansehat.api.ApiClient;
import com.example.sajiansehat.api.ApiService;
import com.example.sajiansehat.models.RekomendasiRequest;
import com.example.sajiansehat.models.RekomendasiResponse;
import com.example.sajiansehat.models.UserProfile;
import com.example.sajiansehat.utils.LocationManager;
import com.example.sajiansehat.utils.ProfileManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RekomendasiFragment extends Fragment {

    private TextInputEditText etBahan, etAlergi, etKesehatan, etDeskripsiBebas;
    private TextInputEditText etUmurRekomendasi, etTinggiRekomendasi, etBeratRekomendasi;
    private TextInputEditText etLocation;
    private AutoCompleteTextView autoCompleteDiet, autoCompleteJenisKelamin;
    private MaterialButton btnCariRekomendasi, btnBatalkan, btnGetLocation;
    private MaterialCheckBox cbSimpanKeProfile, cbExcludeLocation;
    
    private RadioGroup rgTipeMasakan, rgDurasi;
    private MaterialButtonToggleGroup toggleModeInput;
    private LinearLayout layoutTerstruktur, layoutDeskripsi, layoutLoading;
    
    private LinearLayout layoutLockedOverlay;
    private ScrollView layoutLoggedIn;
    private MaterialButton btnLoginSekarang;

    private boolean isModeTerstruktur = true;
    private Call<RekomendasiResponse> currentCall;  // Store current API call for cancellation

    private FirebaseAuth mAuth;
    private LocationManager locationManager;
    private String currentLocationName = "";
    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rekomendasi, container, false);

        mAuth = FirebaseAuth.getInstance();
        locationManager = new LocationManager(requireContext());
        
        // Initialize UI views from layout
        layoutLockedOverlay = view.findViewById(R.id.layout_locked_overlay);
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        btnLoginSekarang = view.findViewById(R.id.btnLoginSekarang);
        
        etBahan = view.findViewById(R.id.etBahan);
        etAlergi = view.findViewById(R.id.etAlergi);
        etKesehatan = view.findViewById(R.id.etKesehatan);
        etDeskripsiBebas = view.findViewById(R.id.etDeskripsiBebas);
        etUmurRekomendasi = view.findViewById(R.id.etUmurRekomendasi);
        etTinggiRekomendasi = view.findViewById(R.id.etTinggiRekomendasi);
        etBeratRekomendasi = view.findViewById(R.id.etBeratRekomendasi);
        etLocation = view.findViewById(R.id.etLocation);
        autoCompleteDiet = view.findViewById(R.id.autoCompleteDiet);
        autoCompleteJenisKelamin = view.findViewById(R.id.autoCompleteJenisKelamin);
        cbSimpanKeProfile = view.findViewById(R.id.cbSimpanKeProfile);
        cbExcludeLocation = view.findViewById(R.id.cbExcludeLocation);
        btnCariRekomendasi = view.findViewById(R.id.btnCariRekomendasi);
        btnGetLocation = view.findViewById(R.id.btnGetLocation);
        
        rgTipeMasakan = view.findViewById(R.id.rgTipeMasakan);
        rgDurasi = view.findViewById(R.id.rgDurasi);
        toggleModeInput = view.findViewById(R.id.toggleModeInput);
        layoutTerstruktur = view.findViewById(R.id.layoutTerstruktur);
        layoutDeskripsi = view.findViewById(R.id.layoutDeskripsi);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        btnBatalkan = view.findViewById(R.id.btnBatalkan);
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            layoutLockedOverlay.setVisibility(View.GONE);
            layoutLoggedIn.setVisibility(View.VISIBLE);
            // Load profile data if available
            loadProfileData();
            // Auto-get location when fragment loads
            getLocationFromGPS();
        } else {
            layoutLockedOverlay.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }

        // Navigate to login when user clicks login button
        btnLoginSekarang.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), LoginActivity.class));
        });

        btnBatalkan.setOnClickListener(v -> {
            // Show confirmation dialog before canceling
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Batalkan Pencarian?")
                    .setMessage("Apakah Anda yakin ingin membatalkan pencarian resep?")
                    .setPositiveButton("Ya, Batalkan", (dialog, which) -> {
                        // Cancel ongoing API request if exists
                        if (currentCall != null) {
                            currentCall.cancel();
                        }
                        resetLoadingState();
                        Toast.makeText(requireContext(), "Pencarian dibatalkan", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Tidak, Lanjutkan", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        });

        // Location button click listener
        btnGetLocation.setOnClickListener(v -> getLocationFromGPS());

        // Initialize jenis kelamin dropdown
        String[] jenisKelaminOptions = new String[]{"Laki-laki", "Perempuan"};
        ArrayAdapter<String> adapterJenisKelamin = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                jenisKelaminOptions
        );
        autoCompleteJenisKelamin.setAdapter(adapterJenisKelamin);

        // Initialize diet options dropdown with predefined diet types
        String[] dietOptions = new String[]{"Tidak Ada Diet Khusus", "Vegetarian", "Vegan", "Keto", "Rendah Karbo", "Bebas Gluten"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                dietOptions
        );
        autoCompleteDiet.setAdapter(adapter);
        autoCompleteDiet.setText(dietOptions[0], false);

        // Toggle between structured form input and free-text description mode
        toggleModeInput.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnModeTerstruktur) {
                    isModeTerstruktur = true;
                    layoutTerstruktur.setVisibility(View.VISIBLE);
                    layoutDeskripsi.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnModeDeskripsi) {
                    isModeTerstruktur = false;
                    layoutTerstruktur.setVisibility(View.GONE);
                    layoutDeskripsi.setVisibility(View.VISIBLE);
                }
            }
        });

        // Handle search button click to fetch recommendations from API
        btnCariRekomendasi.setOnClickListener(v -> {
            // Check network availability before API request
            if (!com.example.sajiansehat.utils.NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), "Tidak ada koneksi internet. Pastikan HP Anda online.", Toast.LENGTH_LONG).show();
                return;
            }
            // Validate input based on mode
            if (isModeTerstruktur) {
                // Validate required fields in structured mode
                if (!validateTerstruktur()) {
                    return;
                }
            } else {
                // Validate description is not empty in free-text mode
                String deskripsi = etDeskripsiBebas.getText() != null ? etDeskripsiBebas.getText().toString().trim() : "";
                if (deskripsi.isEmpty()) {
                    Toast.makeText(requireContext(), "Harap isi deskripsi terlebih dahulu", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Extract duration selection (1 day, 1 week, or 1 month)
            String durasi = "1_hari";
            int selectedDurasiId = rgDurasi.getCheckedRadioButtonId();
            if (selectedDurasiId == R.id.rb1Minggu) {
                durasi = "1_minggu";
            } else if (selectedDurasiId == R.id.rb1Bulan) {
                durasi = "1_bulan";
            }

            // Extract meal type selection (mix, Indonesia, or International)
            String tipe_masakan = "mix";
            int selectedTipeId = rgTipeMasakan.getCheckedRadioButtonId();
            if (selectedTipeId == R.id.rbNasional) {
                tipe_masakan = "indonesia";
            } else if (selectedTipeId == R.id.rbInternasional) {
                tipe_masakan = "internasional";
            }

            // Get or extract profile data
            String jenisKelamin, deskripsi;
            int umur;
            double tinggi, berat;

            if (isModeTerstruktur) {
                // Get from form fields
                jenisKelamin = autoCompleteJenisKelamin.getText().toString();
                umur = Integer.parseInt(etUmurRekomendasi.getText().toString());
                tinggi = Double.parseDouble(etTinggiRekomendasi.getText().toString());
                berat = Double.parseDouble(etBeratRekomendasi.getText().toString());

                // Save to profile if checkbox is checked
                if (cbSimpanKeProfile.isChecked()) {
                    saveToProfile(jenisKelamin, umur, tinggi, berat);
                }

                // Build description from structured form
                String bahan = etBahan.getText() != null ? etBahan.getText().toString().trim() : "";
                String alergi = etAlergi.getText() != null ? etAlergi.getText().toString().trim() : "";
                String kesehatan = etKesehatan.getText() != null ? etKesehatan.getText().toString().trim() : "";
                String diet = autoCompleteDiet.getText().toString();

                deskripsi = "Saya " + jenisKelamin.toLowerCase() + " berusia " + umur + " tahun, tinggi " + tinggi + " cm, berat " + berat + " kg. ";
                deskripsi += "Punya bahan: " + (bahan.isEmpty() ? "Bebas" : bahan) + ". ";
                deskripsi += "Alergi: " + (alergi.isEmpty() ? "Tidak ada" : alergi) + ". ";
                deskripsi += "Kondisi Medis: " + (kesehatan.isEmpty() ? "Sehat" : kesehatan) + ". ";
                deskripsi += "Diet: " + diet + ".";
            } else {
                // Use free-text description
                deskripsi = etDeskripsiBebas.getText().toString().trim();

                // Extract profile data from profile if available
                UserProfile profile = ProfileManager.getProfile(requireContext());
                jenisKelamin = profile.getJenisKelamin() != null && !profile.getJenisKelamin().isEmpty() 
                    ? profile.getJenisKelamin() : "Tidak diketahui";
                umur = profile.getUmur() > 0 ? profile.getUmur() : 0;
                tinggi = profile.getTinggi() > 0 ? profile.getTinggi() : 0;
                berat = profile.getBerat() > 0 ? profile.getBerat() : 0;
            }

            // Switch UI to loading state and disable bottom navigation
            btnCariRekomendasi.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.VISIBLE);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setBottomNavigationEnabled(false);
            }

            // Get location for API request
            String locationForAPI = null;
            if (!currentLocationName.isEmpty() && !cbExcludeLocation.isChecked()) {
                locationForAPI = currentLocationName;
            }

            // Build request object
            RekomendasiRequest requestBody = new RekomendasiRequest(
                autoCompleteJenisKelamin.getText().toString(),
                Integer.parseInt(etUmurRekomendasi.getText().toString()),
                Double.parseDouble(etTinggiRekomendasi.getText().toString()),
                Double.parseDouble(etBeratRekomendasi.getText().toString()),
                durasi,
                tipe_masakan,
                locationForAPI,
                cbExcludeLocation.isChecked(),
                (etAlergi.getText() != null ? etAlergi.getText().toString().trim() : ""),
                (etKesehatan.getText() != null ? etKesehatan.getText().toString().trim() : "")
            );

            final String finalDurasi = durasi;
            final String finalLocation = locationForAPI;

            // ============================================================
            // DIRECT API CALL (Batch Mode - Simple)
            // ============================================================
            
            // Create API service and make direct request
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            currentCall = apiService.getRekomendasi(requestBody);
            
            currentCall.enqueue(new Callback<RekomendasiResponse>() {
                @Override
                public void onResponse(Call<RekomendasiResponse> call, Response<RekomendasiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        RekomendasiResponse rekomendasiResponse = response.body();
                        
                        // Convert response to JSON for passing to activity
                        Gson gson = new Gson();
                        String jsonResponse = gson.toJson(rekomendasiResponse);
                        
                        // Navigate to results activity
                        Intent intent = new Intent(requireContext(), HasilRekomendasiActivity.class);
                        intent.putExtra("DATA_REKOMENDASI", jsonResponse);
                        intent.putExtra("DURASI", finalDurasi);
                        startActivity(intent);
                        
                        resetLoadingState();
                    } else {
                        resetLoadingState();
                        Toast.makeText(requireContext(), 
                            "Gagal mendapatkan rekomendasi: Server error", 
                            Toast.LENGTH_LONG).show();
                    }
                }
                
                @Override
                public void onFailure(Call<RekomendasiResponse> call, Throwable t) {
                    resetLoadingState();
                    if (call.isCanceled()) {
                        Toast.makeText(requireContext(), "Pencarian dibatalkan", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(requireContext(), 
                        "Gagal menghubungi server: " + t.getMessage(), 
                        Toast.LENGTH_LONG).show();
                    Log.e("API_ERROR", "Error: ", t);
                }
            });
        });

        return view;
    }

    private void getLocationFromGPS() {
        // Check permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request permission
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // Permission granted, get location
        locationManager.getLastKnownLocation(new LocationManager.LocationCallback() {
            @Override
            public void onLocationSuccess(String locationName, double latitude, double longitude) {
                currentLocationName = locationName;
                currentLatitude = latitude;
                currentLongitude = longitude;
                
                if (etLocation != null) {
                    etLocation.setText(locationName);
                }
                
                Toast.makeText(requireContext(), "Lokasi: " + locationName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationError(String error) {
                Toast.makeText(requireContext(), "Gagal mendapatkan lokasi: " + error, Toast.LENGTH_SHORT).show();
                Log.e("LOCATION_ERROR", error);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry getting location
                getLocationFromGPS();
            } else {
                Toast.makeText(requireContext(), "Izin akses lokasi ditolak", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update UI based on current login state
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            layoutLockedOverlay.setVisibility(View.GONE);
            layoutLoggedIn.setVisibility(View.VISIBLE);
            loadProfileData();
        } else {
            layoutLockedOverlay.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }
    }

    private void resetLoadingState() {
        // Restore UI to normal state after API request completes
        if (btnCariRekomendasi != null) {
            btnCariRekomendasi.setVisibility(View.VISIBLE);
            btnCariRekomendasi.setText("Analisis & Cari Resep");
            btnCariRekomendasi.setEnabled(true);
        }
        if (layoutLoading != null) {
            layoutLoading.setVisibility(View.GONE);
        }
        // Re-enable bottom navigation after loading
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationEnabled(true);
        }
    }

    private boolean validateTerstruktur() {
        String jenisKelamin = autoCompleteJenisKelamin.getText().toString().trim();
        String umurStr = etUmurRekomendasi.getText() != null ? etUmurRekomendasi.getText().toString().trim() : "";
        String tinggiStr = etTinggiRekomendasi.getText() != null ? etTinggiRekomendasi.getText().toString().trim() : "";
        String beratStr = etBeratRekomendasi.getText() != null ? etBeratRekomendasi.getText().toString().trim() : "";

        if (jenisKelamin.isEmpty()) {
            Toast.makeText(requireContext(), "Harap pilih jenis kelamin", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (umurStr.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi umur", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (tinggiStr.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi tinggi badan", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (beratStr.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi berat badan", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            int umur = Integer.parseInt(umurStr);
            double tinggi = Double.parseDouble(tinggiStr);
            double berat = Double.parseDouble(beratStr);

            if (umur <= 0 || umur > 150) {
                Toast.makeText(requireContext(), "Umur harus antara 1-150 tahun", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (tinggi <= 0 || tinggi > 300) {
                Toast.makeText(requireContext(), "Tinggi badan harus antara 1-300 cm", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (berat <= 0 || berat > 500) {
                Toast.makeText(requireContext(), "Berat badan harus antara 1-500 kg", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Format angka tidak valid", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void loadProfileData() {
        UserProfile profile = ProfileManager.getProfile(requireContext());
        
        if (profile != null) {
            if (profile.getJenisKelamin() != null && !profile.getJenisKelamin().isEmpty()) {
                autoCompleteJenisKelamin.setText(profile.getJenisKelamin(), false);
            }

            if (profile.getUmur() > 0) {
                etUmurRekomendasi.setText(String.valueOf(profile.getUmur()));
            }

            if (profile.getTinggi() > 0) {
                etTinggiRekomendasi.setText(String.valueOf(profile.getTinggi()));
            }

            if (profile.getBerat() > 0) {
                etBeratRekomendasi.setText(String.valueOf(profile.getBerat()));
            }
        }
    }

    private void saveToProfile(String jenisKelamin, int umur, double tinggi, double berat) {
        UserProfile profile = ProfileManager.getProfile(requireContext());
        
        profile.setJenisKelamin(jenisKelamin);
        profile.setUmur(umur);
        profile.setTinggi(tinggi);
        profile.setBerat(berat);
        
        ProfileManager.saveProfile(requireContext(), profile);
        Toast.makeText(requireContext(), "Profile data saved successfully", Toast.LENGTH_SHORT).show();
    }

}

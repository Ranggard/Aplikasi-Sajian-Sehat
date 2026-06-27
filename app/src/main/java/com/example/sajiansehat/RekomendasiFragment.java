package com.example.sajiansehat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sajiansehat.api.ApiClient;
import com.example.sajiansehat.api.ApiService;
import com.example.sajiansehat.models.RekomendasiRequest;
import com.example.sajiansehat.models.RekomendasiResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RekomendasiFragment extends Fragment {

    private TextInputEditText etBahan, etAlergi, etKesehatan, etDeskripsiBebas;
    private AutoCompleteTextView autoCompleteDiet;
    private MaterialButton btnCariRekomendasi, btnBatalkan;
    
    private RadioGroup rgTipeMasakan, rgDurasi;
    private MaterialButtonToggleGroup toggleModeInput;
    private LinearLayout layoutTerstruktur, layoutDeskripsi, layoutLoading;

    private boolean isModeTerstruktur = true;
    private Call<RekomendasiResponse> currentCall;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rekomendasi, container, false);

        // Bind View
        etBahan = view.findViewById(R.id.etBahan);
        etAlergi = view.findViewById(R.id.etAlergi);
        etKesehatan = view.findViewById(R.id.etKesehatan);
        etDeskripsiBebas = view.findViewById(R.id.etDeskripsiBebas);
        autoCompleteDiet = view.findViewById(R.id.autoCompleteDiet);
        btnCariRekomendasi = view.findViewById(R.id.btnCariRekomendasi);
        
        rgTipeMasakan = view.findViewById(R.id.rgTipeMasakan);
        rgDurasi = view.findViewById(R.id.rgDurasi);
        toggleModeInput = view.findViewById(R.id.toggleModeInput);
        layoutTerstruktur = view.findViewById(R.id.layoutTerstruktur);
        layoutDeskripsi = view.findViewById(R.id.layoutDeskripsi);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        btnBatalkan = view.findViewById(R.id.btnBatalkan);

        btnBatalkan.setOnClickListener(v -> {
            if (currentCall != null) {
                currentCall.cancel();
            }
            resetLoadingState();
        });

        // Setup Dropdown (Pilihan Tipe Diet)
        String[] dietOptions = new String[]{"Tidak Ada Diet Khusus", "Vegetarian", "Vegan", "Keto", "Rendah Karbo", "Bebas Gluten"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                dietOptions
        );
        autoCompleteDiet.setAdapter(adapter);
        autoCompleteDiet.setText(dietOptions[0], false);

        // Handle Toggle Mode
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

        // Aksi ketika tombol Cari ditekan
        btnCariRekomendasi.setOnClickListener(v -> {
            if (!com.example.sajiansehat.utils.NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), "Tidak ada koneksi internet. Pastikan HP Anda online.", Toast.LENGTH_LONG).show();
                return;
            }

            // Ambil Tipe Masakan
            String tipeMasakan = "mix";
            int selectedTipeId = rgTipeMasakan.getCheckedRadioButtonId();
            if (selectedTipeId == R.id.rbNasional) {
                tipeMasakan = "indonesia";
            } else if (selectedTipeId == R.id.rbInternasional) {
                tipeMasakan = "internasional";
            }

            // Ambil Durasi
            String durasi = "1_hari";
            int selectedDurasiId = rgDurasi.getCheckedRadioButtonId();
            if (selectedDurasiId == R.id.rb1Minggu) {
                durasi = "1_minggu";
            } else if (selectedDurasiId == R.id.rb1Bulan) {
                durasi = "1_bulan";
            }

            // Rangkai Deskripsi berdasarkan mode
            String finalDeskripsi = "";
            if (isModeTerstruktur) {
                String bahan = etBahan.getText() != null ? etBahan.getText().toString().trim() : "";
                String alergi = etAlergi.getText() != null ? etAlergi.getText().toString().trim() : "";
                String kesehatan = etKesehatan.getText() != null ? etKesehatan.getText().toString().trim() : "";
                String diet = autoCompleteDiet.getText().toString();

                finalDeskripsi = "Saya punya bahan: " + (bahan.isEmpty() ? "Bebas" : bahan) + ". ";
                finalDeskripsi += "Alergi: " + (alergi.isEmpty() ? "Tidak ada" : alergi) + ". ";
                finalDeskripsi += "Kondisi Medis: " + (kesehatan.isEmpty() ? "Sehat" : kesehatan) + ". ";
                finalDeskripsi += "Diet: " + diet + ".";
            } else {
                finalDeskripsi = etDeskripsiBebas.getText() != null ? etDeskripsiBebas.getText().toString().trim() : "";
                if (finalDeskripsi.isEmpty()) {
                    Toast.makeText(requireContext(), "Harap isi deskripsi terlebih dahulu", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Ubah tombol jadi mode loading
            btnCariRekomendasi.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.VISIBLE);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setBottomNavigationEnabled(false);
            }

            // Siapkan Request Body
            RekomendasiRequest requestBody = new RekomendasiRequest(durasi, tipeMasakan, finalDeskripsi);

            final String finalDurasi = durasi;

            // Panggil API Vercel via Retrofit
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            currentCall = apiService.getRekomendasi(requestBody);
            
            currentCall.enqueue(new Callback<RekomendasiResponse>() {
                @Override
                public void onResponse(Call<RekomendasiResponse> call, Response<RekomendasiResponse> response) {
                    resetLoadingState();

                    if (response.isSuccessful() && response.body() != null) {
                        RekomendasiResponse data = response.body();
                        
                        // Konversi object Java ke String JSON
                        Gson gson = new Gson();
                        String jsonResponse = gson.toJson(data);
                        
                        // Berpindah ke halaman Hasil
                        Intent intent = new Intent(requireContext(), HasilRekomendasiActivity.class);
                        intent.putExtra("DATA_REKOMENDASI", jsonResponse);
                        intent.putExtra("DURASI", finalDurasi);
                        startActivity(intent);
                        
                    } else {
                        if (response.code() == 429) {
                            Toast.makeText(requireContext(), "Mohon maaf, kuota pencarian AI sedang penuh. Silakan coba lagi besok.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "Gagal memuat resep dari server", Toast.LENGTH_SHORT).show();
                        }
                        Log.e("API_ERROR", "Response code: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<RekomendasiResponse> call, Throwable t) {
                    resetLoadingState();
                    if (call.isCanceled()) {
                        Toast.makeText(requireContext(), "Pencarian dibatalkan", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Toast.makeText(requireContext(), "Koneksi ke Vercel gagal: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("API_FAIL", "Error: ", t);
                }
            });
        });

        return view;
    }

    private void resetLoadingState() {
        if (btnCariRekomendasi != null) {
            btnCariRekomendasi.setVisibility(View.VISIBLE);
            btnCariRekomendasi.setText("Analisis & Cari Resep");
            btnCariRekomendasi.setEnabled(true);
        }
        if (layoutLoading != null) {
            layoutLoading.setVisibility(View.GONE);
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationEnabled(true);
        }
    }
}
